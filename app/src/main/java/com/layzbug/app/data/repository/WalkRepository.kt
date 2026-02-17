package com.layzbug.app.data.repository

import com.layzbug.app.data.local.WalkDao
import com.layzbug.app.data.local.WalkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton // Important: Makes this a singleton so it persists across ViewModels
class WalkRepository @Inject constructor(
    private val walkDao: WalkDao
) {
    // Cache for month data - persists across ViewModel instances
    private val monthCache = MutableStateFlow<Map<YearMonth, List<WalkEntity>>>(emptyMap())

    fun getWalksInRange(start: LocalDate, end: LocalDate): Flow<List<WalkEntity>> {
        return walkDao.getWalksInRange(start, end)
    }

    fun getWalksForMonth(year: Int, month: Int): Flow<List<WalkEntity>> {
        val yearMonth = YearMonth.of(year, month)

        // Return cached data immediately if available, otherwise query database
        return walkDao.getWalksInRange(
            LocalDate.of(year, month, 1),
            LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth())
        ).map { entities ->
            // Update cache
            val currentCache = monthCache.value.toMutableMap()
            currentCache[yearMonth] = entities
            monthCache.value = currentCache
            entities
        }
    }
    fun getAvailableYears(): Flow<List<Int>> {
        return walkDao.getDistinctYears().map { years ->
            val currentYear = LocalDate.now().year
            // Always include current year even if no walks yet
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

    suspend fun updateWalk(date: LocalDate, isWalked: Boolean) {
        walkDao.upsertWalk(WalkEntity(date, isWalked))

        // Invalidate cache for this month
        val yearMonth = YearMonth.of(date.year, date.month)
        val currentCache = monthCache.value.toMutableMap()
        currentCache.remove(yearMonth)
        monthCache.value = currentCache
    }

    // Get cached month data synchronously for instant display
    fun getCachedMonthData(year: Int, month: Int): List<WalkEntity>? {
        return monthCache.value[YearMonth.of(year, month)]
    }
}