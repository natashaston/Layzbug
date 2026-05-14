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
     * Manual mark — user explicitly tapped a day.
     * Always isManual=true. Always synced to Supabase immediately.
     */
    suspend fun updateManualWalk(date: LocalDate, isWalked: Boolean) {
        val existing         = walkDao.getWalkByDate(date)
        val existingDistance = existing?.distanceKm ?: 0.0
        val existingMinutes  = existing?.minutes ?: 0L

        walkDao.upsertWalk(WalkEntity(date, isWalked, existingDistance, existingMinutes, isManual = true))
        Log.d("WalkRepository", "✅ Manual mark saved to Room: $date = $isWalked, ${existingDistance}km, ${existingMinutes}min")

        monthCache.value = monthCache.value.toMutableMap()
            .also { it.remove(YearMonth.of(date.year, date.month)) }

        supabaseRepository.syncManualWalk(date, isWalked, existingDistance, existingMinutes)
    }

    /**
     * Health Connect update — local only, never touches Supabase.
     *
     * Rules:
     * - Never overwrites a manually marked day's walked status
     * - Minutes never go down (stale HC reads can return less than stored)
     * - Re-evaluates isWalked from finalMinutes so a day qualifying via
     *   accumulated minutes is never left as unwalked
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
            val finalMinutes  = maxOf(minutes, existing?.minutes ?: 0L)
            val finalIsWalked = isWalked || finalMinutes >= 30

            walkDao.upsertWalk(WalkEntity(date, finalIsWalked, finalDistance, finalMinutes, isManual = false))
            Log.d("WalkRepository", "✅ HC update: $date = $finalIsWalked, ${finalDistance}km, ${finalMinutes}min")
            // HC walks are local only — no Supabase sync
        } else {
            // Manual day — preserve walked status, only update stats
            if (distanceKm > 0 || minutes > 0) {
                val newDistance = if (distanceKm > 0) distanceKm else existing?.distanceKm ?: 0.0
                val newMinutes  = maxOf(minutes, existing?.minutes ?: 0L)

                walkDao.upsertWalk(WalkEntity(date, existing!!.isWalked, newDistance, newMinutes, isManual = true))
                Log.d("WalkRepository", "📏 Stats updated for manual day $date: ${newDistance}km, ${newMinutes}min")

                // Re-sync updated stats to Supabase so other devices see correct distance/mins
                repositoryScope.launch {
                    supabaseRepository.syncManualWalk(date, existing.isWalked, newDistance, newMinutes)
                }
            }
        }
    }

    fun getCachedMonthData(year: Int, month: Int): List<WalkEntity>? =
        monthCache.value[YearMonth.of(year, month)]

    /**
     * Push pending manual walks to Supabase after sign-in.
     * Only pushes rows where isManual=true — HC rows (isManual=false) are
     * never pushed, keeping Supabase clean of auto-detected data.
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
     *
     * MERGE RULES — Supabase must never downgrade HC-detected data:
     * 1. finalMinutes  = max(remote, local) — remote can never reduce stored minutes
     * 2. finalIsWalked = remote.isWalked OR finalMinutes >= 30
     *    — HC minutes can upgrade a remote false; remote false never downgrades local true
     * 3. isManual      = remote.isManual (now correctly round-tripped via the new column)
     *    — HC rows (isManual=false in Room) are never in Supabase, so this is always true
     *    for fetched rows, but we store it correctly regardless
     */
    suspend fun syncFromSupabase() {
        Log.d("WalkRepository", "📥 Pulling from Supabase...")
        val remoteWalks = supabaseRepository.fetchAllManualWalks()
        Log.d("WalkRepository", "📦 Fetched ${remoteWalks.size} walks")

        remoteWalks.forEach { walk ->
            val date     = LocalDate.parse(walk.walkDate)
            val existing = walkDao.getWalkByDate(date)

            val localMinutes  = existing?.minutes ?: 0L
            val finalMinutes  = maxOf(walk.minutes, localMinutes)
            val finalIsWalked = walk.isWalked || finalMinutes >= 30
            val finalIsManual = walk.isManual   // correctly preserved from Supabase
            val finalDistance = if (walk.distanceKm > 0) walk.distanceKm else existing?.distanceKm ?: 0.0

            walkDao.upsertWalk(WalkEntity(date, finalIsWalked, finalDistance, finalMinutes, isManual = finalIsManual))
            Log.d("WalkRepository", "  ✅ Pulled: $date = $finalIsWalked (remote=${walk.isWalked} localMin=$localMinutes remoteMin=${walk.minutes}) manual=$finalIsManual")
        }
        Log.d("WalkRepository", "✅ Pull complete")
    }

    /**
     * Real-time Supabase listener — same merge rules as syncFromSupabase().
     */
    fun startSupabaseSync() {
        Log.d("WalkRepository", "👂 Starting real-time listener...")
        repositoryScope.launch {
            supabaseRepository.observeManualWalks().collect { walks ->
                Log.d("WalkRepository", "🔔 Real-time update: ${walks.size} walks")
                walks.forEach { walk ->
                    val date     = LocalDate.parse(walk.walkDate)
                    val existing = walkDao.getWalkByDate(date)

                    val localMinutes  = existing?.minutes ?: 0L
                    val finalMinutes  = maxOf(walk.minutes, localMinutes)
                    val finalIsWalked = walk.isWalked || finalMinutes >= 30
                    val finalIsManual = walk.isManual
                    val finalDistance = if (walk.distanceKm > 0) walk.distanceKm else existing?.distanceKm ?: 0.0

                    walkDao.upsertWalk(WalkEntity(date, finalIsWalked, finalDistance, finalMinutes, isManual = finalIsManual))
                    Log.d("WalkRepository", "  🔔 RT merged: $date = $finalIsWalked (remote=${walk.isWalked} localMin=$localMinutes remoteMin=${walk.minutes})")
                }
            }
        }
    }

    fun isSupabaseLoggedIn(): Boolean = supabaseRepository.isLoggedIn
}