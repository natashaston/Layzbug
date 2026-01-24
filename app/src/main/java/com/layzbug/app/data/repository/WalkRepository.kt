package com.layzbug.app.data.repository

import com.layzbug.app.data.local.WalkDao
import com.layzbug.app.data.local.WalkEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class WalkRepository @Inject constructor(
    private val walkDao: WalkDao
) {
    // This fixes the "Unresolved reference: getWalksInRange"
    fun getWalksInRange(start: LocalDate, end: LocalDate): Flow<List<WalkEntity>> {
        return walkDao.getWalksInRange(start, end)
    }

    // Keep your existing month query for the January card
    fun getWalksForMonth(year: Int, month: Int): Flow<List<WalkEntity>> {
        val query = String.format("%04d-%02d", year, month)
        // We can reuse the range logic here or keep the LIKE query
        return walkDao.getWalksInRange(
            LocalDate.of(year, month, 1),
            LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth())
        )
    }

    suspend fun getWalkStatus(date: LocalDate): Boolean {
        return walkDao.getWalkStatus(date) ?: false
    }

    suspend fun updateWalk(date: LocalDate, isWalked: Boolean) {
        walkDao.upsertWalk(WalkEntity(date, isWalked))
    }
}