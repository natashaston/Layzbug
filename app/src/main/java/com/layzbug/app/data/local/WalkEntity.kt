package com.layzbug.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "walks")
data class WalkEntity(
    @PrimaryKey val date: LocalDate,
    val isWalked: Boolean,
    val distanceKm: Double = 0.0,
    val minutes: Long = 0L,
    val isManual: Boolean = false   // true = user manually marked this day
)