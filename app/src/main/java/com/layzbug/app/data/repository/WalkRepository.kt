package com.layzbug.app.data.repository

import android.util.Log
import com.layzbug.app.data.local.WalkDao
import com.layzbug.app.data.local.WalkEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    fun getAvailableYears(): Flow<List<Int>> {
        return walkDao.getDistinctYears().map { years ->
            val currentYear = LocalDate.now().year
            val dbYears = years.map { it.toInt() }.toMutableList()
            if (!dbYears.contains(currentYear)) dbYears.add(0, currentYear)
            dbYears.sorted().reversed()
        }
    }

    suspend fun getWalkStatus(date: LocalDate): Boolean =
        walkDao.getWalkStatus(date) ?: false

    /**
     * Manual mark — persists isManual=true in Room so it survives app restarts.
     * Syncs immediately to Supabase if logged in; if not, syncPendingManualWalks()
     * will push it after sign-in.
     */
    suspend fun updateManualWalk(date: LocalDate, isWalked: Boolean) {
        val existing = walkDao.getWalkByDate(date)
        val existingDistance = existing?.distanceKm ?: 0.0
        val existingMinutes  = existing?.minutes ?: 0L

        // isManual=true persisted to Room — survives restarts
        walkDao.upsertWalk(WalkEntity(date, isWalked, existingDistance, existingMinutes, isManual = true))
        Log.d("WalkRepository", "✅ Manual mark saved to Room: $date = $isWalked, ${existingDistance}km, ${existingMinutes}min")

        // Invalidate cache
        monthCache.value = monthCache.value.toMutableMap()
            .also { it.remove(YearMonth.of(date.year, date.month)) }

        // Sync to Supabase immediately — upserts in both walked and unwalked directions
        repositoryScope.launch {
            Log.d("WalkRepository", "🔄 Syncing to Supabase: $date = $isWalked")
            supabaseRepository.syncManualWalk(date, isWalked, existingDistance, existingMinutes)
        }
    }

    /**
     * Health Connect update — never overwrites manual walked status.
     * Always updates distance/minutes and re-syncs to Supabase if manual.
     */
    suspend fun updateWalkFromGoogleFit(
        date: LocalDate,
        isWalked: Boolean,
        distanceKm: Double = 0.0,
        minutes: Long = 0L
    ) {
        val existing = walkDao.getWalkByDate(date)
        val isManual = existing?.isManual ?: false

        if (!isManual) {
            val finalDistance = if (distanceKm > 0) distanceKm else existing?.distanceKm ?: 0.0
            walkDao.upsertWalk(WalkEntity(date, isWalked, finalDistance, minutes, isManual = false))
            Log.d("WalkRepository", "✅ Health Connect update: $date = $isWalked, ${finalDistance}km, ${minutes}min")
        } else {
            // Manual day — preserve walked status, only update stats
            if (distanceKm > 0 || minutes > 0) {
                val newDistance = if (distanceKm > 0) distanceKm else existing?.distanceKm ?: 0.0
                val newMinutes  = if (minutes > 0) minutes else existing?.minutes ?: 0L
                walkDao.upsertWalk(WalkEntity(date, existing!!.isWalked, newDistance, newMinutes, isManual = true))
                Log.d("WalkRepository", "📏 Stats updated for manual day $date: ${newDistance}km, ${newMinutes}min")

                // Re-sync updated stats to Supabase
                repositoryScope.launch {
                    supabaseRepository.syncManualWalk(date, existing.isWalked, newDistance, newMinutes)
                }
            }
        }
    }

    fun getCachedMonthData(year: Int, month: Int): List<WalkEntity>? =
        monthCache.value[YearMonth.of(year, month)]

    /**
     * Push all locally stored manual walks to Supabase.
     * Reads from Room (persisted) so it works after app restarts.
     * Called right after sign-in.
     */
    suspend fun syncPendingManualWalks() {
        Log.d("WalkRepository", "📤 Pushing pending manual walks to Supabase...")
        val manualWalks = walkDao.getAllManualWalks()
        Log.d("WalkRepository", "📤 Found ${manualWalks.size} manual walks to push")
        manualWalks.forEach { entity ->
            supabaseRepository.syncManualWalk(
                date       = entity.date,
                isWalked   = entity.isWalked,
                distanceKm = entity.distanceKm,
                minutes    = entity.minutes
            )
            Log.d("WalkRepository", "  ✅ Pushed: ${entity.date} = ${entity.isWalked}, ${entity.distanceKm}km, ${entity.minutes}min")
        }
        Log.d("WalkRepository", "✅ Pending sync complete")
    }

    /**
     * Pull manual walks from Supabase into Room.
     * Called after sign-in.
     */
    suspend fun syncFromSupabase() {
        Log.d("WalkRepository", "📥 Pulling from Supabase...")
        val remoteWalks = supabaseRepository.fetchAllManualWalks()
        Log.d("WalkRepository", "📦 Fetched ${remoteWalks.size} walks")

        remoteWalks.forEach { walk ->
            val date     = LocalDate.parse(walk.walkDate)
            val existing = walkDao.getWalkByDate(date)
            val minutes  = if (walk.minutes > 0) walk.minutes else existing?.minutes ?: 0L
            walkDao.upsertWalk(WalkEntity(date, walk.isWalked, walk.distanceKm, minutes, isManual = true))
            Log.d("WalkRepository", "  ✅ Pulled: $date = ${walk.isWalked}, ${walk.distanceKm}km, ${minutes}min")
        }
        Log.d("WalkRepository", "✅ Pull complete")
    }

    /**
     * Real-time Supabase listener.
     */
    fun startSupabaseSync() {
        Log.d("WalkRepository", "👂 Starting real-time listener...")
        repositoryScope.launch {
            supabaseRepository.observeManualWalks().collect { walks ->
                Log.d("WalkRepository", "🔔 Real-time update: ${walks.size} walks")
                walks.forEach { walk ->
                    val date     = LocalDate.parse(walk.walkDate)
                    val existing = walkDao.getWalkByDate(date)
                    val minutes  = if (walk.minutes > 0) walk.minutes else existing?.minutes ?: 0L
                    walkDao.upsertWalk(WalkEntity(date, walk.isWalked, walk.distanceKm, minutes, isManual = true))
                }
            }
        }
    }

    fun isSupabaseLoggedIn(): Boolean = supabaseRepository.isLoggedIn
}