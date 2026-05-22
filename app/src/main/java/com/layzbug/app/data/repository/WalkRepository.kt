package com.layzbug.app.data.repository

import android.util.Log
import com.layzbug.app.WalkSegment
import com.layzbug.app.data.local.WalkDao
import com.layzbug.app.data.local.WalkEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalkRepository @Inject constructor(
    private val walkDao: WalkDao,
    private val supabaseRepository: SupabaseRepository
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val monthCache = MutableStateFlow<Map<YearMonth, List<WalkEntity>>>(emptyMap())

    fun getWalksInRange(start: LocalDate, end: LocalDate): Flow<List<WalkEntity>> =
        walkDao.getWalksInRange(start, end)

    fun getWalksForMonth(year: Int, month: Int): Flow<List<WalkEntity>> {
        val yearMonth = YearMonth.of(year, month)
        return walkDao.getWalksInRange(
            LocalDate.of(year, month, 1),
            LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth())
        ).map { entities ->
            monthCache.value = monthCache.value.toMutableMap().also { it[yearMonth] = entities }
            entities
        }
    }

    suspend fun getWalkStatus(date: LocalDate): Boolean =
        walkDao.getWalkStatus(date) ?: false

    suspend fun updateManualWalk(date: LocalDate, isWalked: Boolean) {
        val existing         = walkDao.getWalkByDate(date)
        val existingDistance = existing?.distanceKm ?: 0.0
        val existingMinutes  = existing?.minutes ?: 0L
        val existingSegments = existing?.segments ?: emptyList()

        walkDao.upsertWalk(WalkEntity(date, isWalked, existingDistance, existingMinutes, isManual = true, segments = existingSegments))

        monthCache.value = monthCache.value.toMutableMap()
            .also { it.remove(YearMonth.of(date.year, date.month)) }

        supabaseRepository.syncManualWalk(date, isWalked, existingDistance, existingMinutes)
    }

    suspend fun updateWalkFromGoogleFit(
        date: LocalDate,
        isWalked: Boolean,
        distanceKm: Double = 0.0,
        minutes: Long = 0L,
        segments: List<WalkSegment> = emptyList()
    ) {
        val existing = walkDao.getWalkByDate(date)
        val isManual = existing?.isManual ?: false

        val finalMinutes = if (segments.isNotEmpty()) {
            segments.filter { it.isQualified }.sumOf { it.durationMinutes }
        } else {
            maxOf(minutes, existing?.minutes ?: 0L)
        }

        val finalSegments = if (segments.isEmpty() && existing != null) {
            existing.segments
        } else {
            segments
        }

        val finalDistance = if (distanceKm > 0) distanceKm else existing?.distanceKm ?: 0.0
        val finalIsWalked = isWalked || finalMinutes >= 30

        if (!isManual) {
            walkDao.upsertWalk(WalkEntity(date, finalIsWalked, finalDistance, finalMinutes, isManual = false, segments = finalSegments))
        } else {
            walkDao.upsertWalk(WalkEntity(date, existing!!.isWalked, finalDistance, finalMinutes, isManual = true, segments = finalSegments))

            repositoryScope.launch {
                supabaseRepository.syncManualWalk(date, existing.isWalked, finalDistance, finalMinutes)
            }
        }

        monthCache.value = monthCache.value.toMutableMap()
            .also { it.remove(YearMonth.of(date.year, date.month)) }
    }

    suspend fun updateWalkFromStepDetector(
        date: LocalDate,
        startTime: Instant,
        endTime: Instant,
        durationMinutes: Long,
        stepCount: Long,
        stepsPerMinute: Long,
        isQualified: Boolean,
        rejectReason: String?
    ) {
        val existing = walkDao.getWalkByDate(date)
        val isManual = existing?.isManual ?: false

        val newSegment = WalkSegment(
            durationMinutes = durationMinutes,
            isQualified     = isQualified,
            rejectReason    = rejectReason,
            stepCount       = stepCount,
            stepsPerMinute  = stepsPerMinute
        )

        val allSegments = (existing?.segments ?: emptyList()) + newSegment
        val totalQualifiedMinutes = allSegments.filter { it.isQualified }.sumOf { it.durationMinutes }

        val finalIsWalked = if (isManual) existing!!.isWalked else totalQualifiedMinutes >= 30
        val existingDistance = existing?.distanceKm ?: 0.0

        walkDao.upsertWalk(WalkEntity(date, finalIsWalked, existingDistance, totalQualifiedMinutes, isManual = isManual, segments = allSegments))

        monthCache.value = monthCache.value.toMutableMap()
            .also { it.remove(YearMonth.of(date.year, date.month)) }
    }

    fun isSupabaseLoggedIn(): Boolean = supabaseRepository.isLoggedIn

    fun getAvailableYears(): Flow<List<Int>> =
        walkDao.getDistinctYears().map { it.map { y -> y.toInt() }.sortedDescending() }

    fun getCachedMonthData(year: Int, month: Int): List<WalkEntity>? =
        monthCache.value[YearMonth.of(year, month)]

    suspend fun syncPendingManualWalks() {
        Log.d("WalkRepository", "📤 Pushing pending manual walks to Supabase...")
        val manualWalks = walkDao.getAllManualWalks()
        manualWalks.forEach { entity ->
            supabaseRepository.syncManualWalk(
                date       = entity.date,
                isWalked   = entity.isWalked,
                distanceKm = entity.distanceKm,
                minutes    = entity.minutes
            )
        }
        Log.d("WalkRepository", "✅ Pending sync complete")
    }

    suspend fun syncFromSupabase() {
        Log.d("WalkRepository", "📥 Pulling manual walks from Supabase...")
        val remoteWalks = supabaseRepository.fetchAllManualWalks()

        remoteWalks.forEach { walk ->
            val date     = LocalDate.parse(walk.walkDate)
            val existing = walkDao.getWalkByDate(date)

            // Don't overwrite a local manual walk with cloud data
            if (existing?.isManual == true) return@forEach

            walkDao.upsertWalk(
                WalkEntity(
                    date       = date,
                    isWalked   = walk.isWalked,
                    distanceKm = walk.distanceKm,
                    minutes    = walk.minutes,
                    isManual   = true,
                    segments   = existing?.segments ?: emptyList()
                )
            )
            Log.d("WalkRepository", "  ✅ Synced manual: $date = ${walk.isWalked}")
        }
        Log.d("WalkRepository", "✅ Supabase manual sync complete")
    }

    fun startSupabaseSync() {
        Log.d("WalkRepository", "👂 Starting Supabase real-time listener...")
        repositoryScope.launch {
            supabaseRepository.observeManualWalks().collect { walks ->
                Log.d("WalkRepository", "🔔 Supabase update: ${walks.size} manual walks")
                walks.forEach { walk ->
                    val date     = LocalDate.parse(walk.walkDate)
                    val existing = walkDao.getWalkByDate(date)
                    if (existing?.isManual == true) return@forEach
                    walkDao.upsertWalk(
                        WalkEntity(
                            date       = date,
                            isWalked   = walk.isWalked,
                            distanceKm = walk.distanceKm,
                            minutes    = walk.minutes,
                            isManual   = true,
                            segments   = existing?.segments ?: emptyList()
                        )
                    )
                }
            }
        }
    }

    suspend fun restoreAutoWalksFromSupabase() {
        Log.d("WalkRepository", "📥 Restoring auto walks from Supabase...")
        val remoteSessions = supabaseRepository.fetchAllAutoWalkSessions()

        if (remoteSessions.isEmpty()) {
            Log.d("WalkRepository", "No auto walks in Supabase to restore")
            return
        }

        val byDate = remoteSessions.groupBy { LocalDate.parse(it.walkDate) }

        byDate.forEach { (date, sessions) ->
            val qualifiedMinutes = sessions
                .filter { it.isQualified }
                .sumOf { it.durationMinutes }

            val totalDistanceKm = Math.round(
                sessions.sumOf { it.distanceKm } * 100.0
            ) / 100.0

            val isWalked = qualifiedMinutes >= 30

            val segments = sessions.map { session ->
                WalkSegment(
                    durationMinutes = session.durationMinutes,
                    isQualified     = session.isQualified,
                    rejectReason    = session.rejectReason,
                    stepCount       = session.stepCount,
                    stepsPerMinute  = session.stepsPerMinute
                )
            }

            val existing = walkDao.getWalkByDate(date)
            if (existing == null || !existing.isManual) {
                walkDao.upsertWalk(
                    WalkEntity(
                        date       = date,
                        isWalked   = isWalked,
                        distanceKm = totalDistanceKm,
                        minutes    = qualifiedMinutes,
                        isManual   = false,
                        segments   = segments
                    )
                )
            }
        }

        Log.d("WalkRepository", "✅ Restored ${byDate.size} days from Supabase auto walks")
    }
}