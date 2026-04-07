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

    // Track which walks are manual vs Google Fit
    private val manualWalks = mutableSetOf<LocalDate>()

    fun getWalksInRange(start: LocalDate, end: LocalDate): Flow<List<WalkEntity>> {
        return walkDao.getWalksInRange(start, end)
    }

    fun getWalksForMonth(year: Int, month: Int): Flow<List<WalkEntity>> {
        val yearMonth = YearMonth.of(year, month)
        return walkDao.getWalksInRange(
            LocalDate.of(year, month, 1),
            LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth())
        ).map { entities ->
            val currentCache = monthCache.value.toMutableMap()
            currentCache[yearMonth] = entities
            monthCache.value = currentCache
            entities
        }
    }

    fun getAvailableYears(): Flow<List<Int>> {
        return walkDao.getDistinctYears().map { years ->
            val currentYear = LocalDate.now().year
            val dbYears = years.map { it.toInt() }.toMutableList()
            if (!dbYears.contains(currentYear)) {
                dbYears.add(0, currentYear)
            }
            dbYears.sorted().reversed()
        }
    }

    suspend fun getWalkStatus(date: LocalDate): Boolean {
        return walkDao.getWalkStatus(date) ?: false
    }

    /**
     * Update walk - for MANUAL marks only
     * Google Fit walks are updated via updateWalkFromGoogleFit()
     */
    suspend fun updateManualWalk(date: LocalDate, isWalked: Boolean) {
        // Update local DB (manual walks don't have distance — keep existing distance or 0)
        val existingDistance = try {
            val walks = walkDao.getWalksInRange(date, date)
            var dist = 0.0
            // We can't collect a Flow here easily, so we just set 0 for manual
            dist
        } catch (e: Exception) {
            0.0
        }

        walkDao.upsertWalk(WalkEntity(date, isWalked, existingDistance))
        Log.d("WalkRepository", "✅ Updated local DB (manual): $date = $isWalked")

        // Track as manual walk
        if (isWalked) {
            manualWalks.add(date)
        } else {
            manualWalks.remove(date)
        }

        // Invalidate cache
        val yearMonth = YearMonth.of(date.year, date.month)
        val currentCache = monthCache.value.toMutableMap()
        currentCache.remove(yearMonth)
        monthCache.value = currentCache

        // Sync manual walk to Supabase
        repositoryScope.launch {
            Log.d("WalkRepository", "🔄 Syncing manual walk to Supabase: $date = $isWalked")

            if (isWalked) {
                supabaseRepository.syncManualWalk(date, true)
            } else {
                supabaseRepository.deleteManualWalk(date)
            }
        }
    }

    /**
     * Update walk from Google Fit - does NOT sync to Supabase
     * Now includes distance data
     */
    suspend fun updateWalkFromGoogleFit(date: LocalDate, isWalked: Boolean, distanceKm: Double = 0.0) {
        // Only update if not already a manual walk
        if (!manualWalks.contains(date)) {
            val existing = walkDao.getWalkByDate(date)
            // Trust Google Fit: always use the latest result from the algorithm
            // (allows correcting previously walked days when rules change)
            val finalIsWalked = isWalked
            // Always update distance if we have a better value
            val finalDistance = if (distanceKm > 0) distanceKm
            else existing?.distanceKm ?: 0.0

            walkDao.upsertWalk(WalkEntity(date, finalIsWalked, finalDistance))
            Log.d("WalkRepository", "✅ Updated from Google Fit: $date = $finalIsWalked (was: ${existing?.isWalked}), ${finalDistance}km")
        } else {
            // For manual walks, still update distance if we have it
            if (distanceKm > 0) {
                val existing = walkDao.getWalkByDate(date)
                if (existing != null && existing.distanceKm == 0.0) {
                    walkDao.upsertWalk(WalkEntity(date, existing.isWalked, distanceKm))
                    Log.d("WalkRepository", "📏 Updated distance for manual walk $date: ${distanceKm}km")
                }
            } else {
                Log.d("WalkRepository", "⏭️ Skipped $date - is manual walk")
            }
        }
    }

    fun getCachedMonthData(year: Int, month: Int): List<WalkEntity>? {
        return monthCache.value[YearMonth.of(year, month)]
    }

    /**
     * Sync manual walks FROM Supabase (called after login)
     */
    suspend fun syncFromSupabase() {
        Log.d("WalkRepository", "📥 Syncing manual walks from Supabase...")

        val manualWalksFromCloud = supabaseRepository.fetchAllManualWalks()
        Log.d("WalkRepository", "📦 Fetched ${manualWalksFromCloud.size} manual walks")

        manualWalksFromCloud.forEach { walk ->
            val date = LocalDate.parse(walk.walkDate)

            // Mark as manual walk
            if (walk.isWalked) {
                manualWalks.add(date)
            }

            // Update local DB with distance from cloud
            walkDao.upsertWalk(WalkEntity(date, walk.isWalked, walk.distanceKm))
            Log.d("WalkRepository", "  ✅ Synced: $date = ${walk.isWalked}, ${walk.distanceKm}km")
        }

        Log.d("WalkRepository", "✅ Supabase sync complete")
    }

    /**
     * Start observing Supabase changes (real-time)
     */
    fun startSupabaseSync() {
        Log.d("WalkRepository", "👂 Starting Supabase real-time listener...")

        repositoryScope.launch {
            supabaseRepository.observeManualWalks().collect { walks ->
                Log.d("WalkRepository", "🔔 Supabase update: ${walks.size} manual walks")

                walks.forEach { walk ->
                    val date = LocalDate.parse(walk.walkDate)

                    // Mark as manual walk
                    if (walk.isWalked) {
                        manualWalks.add(date)
                    }

                    // Update local DB with distance
                    walkDao.upsertWalk(WalkEntity(date, walk.isWalked, walk.distanceKm))
                }
            }
        }
    }

    /**
     * Check if user is logged into Supabase (has Google account)
     */
    fun isSupabaseLoggedIn(): Boolean {
        return supabaseRepository.isLoggedIn
    }
}