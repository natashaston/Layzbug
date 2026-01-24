package com.layzbug.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class FitSyncManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun checkWalkingGoal(date: LocalDate): Boolean {
        return try {
            val startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = startTime.plus(1, ChronoUnit.DAYS)

            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val totalSteps = response.records.sumOf { it.count }
            // 3000 steps = 30-minute walk proxy
            totalSteps >= 3000
        } catch (e: Exception) {
            false
        }
    }

    // ... rest of your existing big code ...
}