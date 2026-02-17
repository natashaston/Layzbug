package com.layzbug.app.data.repository

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
    private val firebaseRepository: FirebaseRepository
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val monthCache = MutableStateFlow<Map<YearMonth, List<WalkEntity>>>(emptyMap())

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

    suspend fun updateWalk(date: LocalDate, isWalked: Boolean) {
        // Always update local DB first
        walkDao.upsertWalk(WalkEntity(date, isWalked))

        // Invalidate cache
        val yearMonth = YearMonth.of(date.year, date.month)
        val currentCache = monthCache.value.toMutableMap()
        currentCache.remove(yearMonth)
        monthCache.value = currentCache

        // Sync to Firebase if logged in (fire and forget)
        if (firebaseRepository.isLoggedIn) {
            repositoryScope.launch {
                firebaseRepository.syncWalkToFirebase(date, isWalked)
            }
        }
    }

    fun getCachedMonthData(year: Int, month: Int): List<WalkEntity>? {
        return monthCache.value[YearMonth.of(year, month)]
    }

    // Sync all Firebase walks to local DB (called after login)
    suspend fun syncFromFirebase() {
        val firebaseWalks = firebaseRepository.fetchAllWalks()
        firebaseWalks.forEach { entry ->
            val date = LocalDate.parse(entry.date)
            walkDao.upsertWalk(WalkEntity(date, entry.isWalked))
        }
    }

    // Start observing Firestore changes (called after login)
    fun startFirebaseSync() {
        repositoryScope.launch {
            firebaseRepository.observeWalks().collect { firebaseWalks ->
                firebaseWalks.forEach { entry ->
                    val date = LocalDate.parse(entry.date)
                    walkDao.upsertWalk(WalkEntity(date, entry.isWalked))
                }
            }
        }
    }
}