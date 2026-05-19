package com.layzbug.app.data.repository

import android.util.Log
import com.layzbug.app.WalkSegment
import com.layzbug.app.data.FitSession
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

        // Fix: If segments are present, use their qualified totals.
        // If segments are empty (e.g. historical data over 60 days old), trust the incoming minutes.
        val finalMinutes = if (segments.isNotEmpty()) {
            segments.filter { it.isQualified }.sumOf { it.durationMinutes }
        } else {
            maxOf(minutes, existing?.minutes ?: 0L)
        }

        // Retain older segments if the new pass returned nothing but we have past records
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

    suspend fun updateWalkFromFitBackfill(sessions: List<FitSession>) {
        if (sessions.isEmpty()) return
        val byDate = sessions.groupBy { it.date }

        byDate.forEach { (date, daySessions) ->
            val qualifiedMinutes = daySessions.filter { it.isQualified }.sumOf { it.durationMinutes }
            val totalDistanceKm = Math.round(daySessions.sumOf { it.distanceKm } * 100.0) / 100.0
            val isWalked = qualifiedMinutes >= 30

            val segments = daySessions.map { session ->
                WalkSegment(
                    durationMinutes = session.durationMinutes,
                    isQualified     = session.isQualified,
                    rejectReason    = session.rejectReason,
                    stepCount       = session.stepCount,
                    stepsPerMinute  = session.stepsPerMinute
                )
            }

            val existing = walkDao.getWalkByDate(date)
            val isManual = existing?.isManual ?: false

            if (!isManual) {
                walkDao.upsertWalk(WalkEntity(date, isWalked, totalDistanceKm, qualifiedMinutes, isManual = false, segments = segments))
            } else {
                walkDao.upsertWalk(WalkEntity(date, existing!!.isWalked, totalDistanceKm, qualifiedMinutes, isManual = true, segments = segments))
            }
        }

        val affectedMonths = byDate.keys.map { YearMonth.of(it.year, it.month) }.toSet()
        monthCache.value = monthCache.value.toMutableMap().also { cache ->
            affectedMonths.forEach { cache.remove(it) }
        }
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
    fun hasGoogleFitBackfill(): suspend () -> Boolean = { supabaseRepository.hasGoogleFitBackfillCompleted() }
    fun getAvailableYears(): Flow<List<Int>> = walkDao.getDistinctYears().map { it.map { y -> y.toInt() }.sortedDescending() }
    fun getCachedMonthData(year: Int, month: Int): List<WalkEntity>? = monthCache.value[YearMonth.of(year, month)]
    suspend fun syncPendingManualWalks() {}
    suspend fun syncFromSupabase() {}
    suspend fun startSupabaseSync() {}
    suspend fun restoreAutoWalksFromSupabase() {}
}