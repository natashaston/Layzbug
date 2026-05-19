package com.layzbug.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.layzbug.app.WalkSegment
import java.time.LocalDate

@Entity(tableName = "walks")
data class WalkEntity(
    @PrimaryKey val date: LocalDate,
    val isWalked: Boolean,
    val distanceKm: Double = 0.0,
    val minutes: Long = 0L,
    val isManual: Boolean = false,
    val segments: List<WalkSegment> = emptyList() // Clean, no annotations needed here
)