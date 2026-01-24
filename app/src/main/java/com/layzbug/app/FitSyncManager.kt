package com.layzbug.app

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest // Change here
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FitSyncManager @Inject constructor(
    private val healthConnectClient: HealthConnectClient
) {
    suspend fun checkWalkingGoal(date: LocalDate): Boolean {
        try {
            val startTime = date.atStartOfDay().toInstant(ZoneOffset.UTC)
            val endTime = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            val timeRange = TimeRangeFilter.between(startTime, endTime)

            // 1. Check for manual sessions (Google Fit "Workouts")
            val sessionResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = timeRange
                )
            )
            if (sessionResponse.records.any { Duration.between(it.startTime, it.endTime).toMinutes() >= 30 }) {
                return true
            }

            // 2. City-Proof Streak Logic using aggregateGroupByDuration
            val response = healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeRange,
                    timeRangeSlicer = Duration.ofMinutes(1) // Duration works here!
                )
            )

            var continuousMinutes = 0
            var gapMinutes = 0
            val maxAllowedGap = 4 // Your 3-minute traffic stops are safe

            for (bucket in response) {
                val stepsInMinute = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L

                if (stepsInMinute >= 40) {
                    continuousMinutes += (1 + gapMinutes)
                    gapMinutes = 0

                    if (continuousMinutes >= 30) return true
                } else {
                    gapMinutes++
                    if (gapMinutes > maxAllowedGap) {
                        continuousMinutes = 0
                        gapMinutes = 0
                    }
                }
            }

            return false
        } catch (e: Exception) {
            Log.e("FitSync", "Error for $date: ${e.message}")
            return false
        }
    }
}