package com.layzbug.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "walks")
data class WalkEntity(
    @PrimaryKey val date: LocalDate, // Room will use your Converters to save this as a String
    val isWalked: Boolean
)