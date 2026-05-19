package com.layzbug.app.data.local

import androidx.room.TypeConverter
import com.layzbug.app.WalkSegment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WalkConverters {
    @TypeConverter
    fun fromWalkSegmentList(value: List<WalkSegment>?): String {
        if (value == null) return "[]"
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toWalkSegmentList(value: String?): List<WalkSegment> {
        if (value.isNullOrEmpty() || value == "null") return emptyList()
        return try {
            Json.decodeFromString<List<WalkSegment>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}