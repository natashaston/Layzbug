package com.layzbug.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WalkDao {
    @Query("SELECT * FROM walks WHERE date BETWEEN :startDate AND :endDate")
    fun getWalksInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WalkEntity>>

    @Query("SELECT isWalked FROM walks WHERE date = :date LIMIT 1")
    suspend fun getWalkStatus(date: LocalDate): Boolean?

    @Upsert
    suspend fun upsertWalk(walk: WalkEntity)
}