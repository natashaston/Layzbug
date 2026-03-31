package com.layzbug.app

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class DailyWalkResult(
    val isWalked: Boolean,
    val distanceKm: Double,
    val totalMinutes: Long = 0,
    val sessionCount: Int = 0
)

@Singleton
class FitSyncManager @Inject constructor(
    private val healthConnectClient: HealthConnectClient
) {
    suspend fun hasPermissions(permissions: Set<String>): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            Log.e("FitSync", "Error checking permissions: ${e.message}")
            false
        }
    }

    suspend fun checkDailyWalk(date: LocalDate): DailyWalkResult {
        try {
            val zoneId = ZoneId.systemDefault()
            val startTime = date.atStartOfDay(zoneId).toInstant()
            val endTime = date.plusDays(1).atStartOfDay(zoneId).toInstant()
            val timeRange = TimeRangeFilter.between(startTime, endTime)

            // 1. Check for manual sessions (Google Fit "Workouts")
            val sessionResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = timeRange
                )
            )

            val sessionMinutes = sessionResponse.records.sumOf { session ->
                Duration.between(session.startTime, session.endTime).toMinutes()
            }

            var isWalked = sessionMinutes >= 30

            // 2. If no exercise sessions met the goal, use City-Proof Streak Logic
            if (!isWalked) {
                isWalked = checkStepStreak(timeRange)
            }

            // 3. Aggregate total distance for the day
            val distanceKm = try {
                val aggregateResponse = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = timeRange
                    )
                )
                val distanceMeters = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                Math.round(distanceMeters / 1000.0 * 100.0) / 100.0
            } catch (e: Exception) {
                Log.e("FitSync", "Error reading distance for $date: ${e.message}")
                0.0
            }

            Log.d("FitSync", "📊 $date: ${sessionResponse.records.size}sess/${sessionMinutes}min, " +
                    "${distanceKm}km, walked=$isWalked")

            return DailyWalkResult(
                isWalked = isWalked,
                distanceKm = distanceKm,
                totalMinutes = sessionMinutes,
                sessionCount = sessionResponse.records.size
            )
        } catch (e: Exception) {
            Log.e("FitSync", "Error for $date: ${e.message}", e)
            return DailyWalkResult(isWalked = false, distanceKm = 0.0)
        }
    }

    /**
     * Walk detection using two strategies:
     * 1. Continuous streak >= 30 min (with 4-min gap tolerance) — catches single long walks
     * 2. Sum all walk segments >= 5 min. If total >= 30 min, mark as walked.
     *    Segments under 5 min are discarded (kitchen, bathroom, room-to-room).
     */
    private suspend fun checkStepStreak(timeRange: TimeRangeFilter): Boolean {
        return try {
            val response = healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeRange,
                    timeRangeSlicer = Duration.ofMinutes(1)
                )
            )

            // Track all walk segments (continuous blocks of walking with gap tolerance)
            var continuousMinutes = 0
            var gapMinutes = 0
            val maxAllowedGap = 4
            val segments = mutableListOf<Int>()  // Duration of each walk segment

            for (bucket in response) {
                val stepsInMinute = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L

                if (stepsInMinute >= 40) {
                    continuousMinutes += (1 + gapMinutes)
                    gapMinutes = 0

                    // Strategy 1: Single continuous 30-min walk
                    if (continuousMinutes >= 30) {
                        Log.d("FitSync", "🚶 Continuous streak: ${continuousMinutes}min")
                        return true
                    }
                } else {
                    gapMinutes++
                    if (gapMinutes > maxAllowedGap) {
                        // Segment ended — record it
                        if (continuousMinutes > 0) {
                            segments.add(continuousMinutes)
                        }
                        continuousMinutes = 0
                        gapMinutes = 0
                    }
                }
            }

            // Don't forget the last segment
            if (continuousMinutes > 0) {
                segments.add(continuousMinutes)
            }

            // Strategy 2: Only count segments >= 5 min as real walks
            // Discard anything under 5 min (kitchen, bathroom, room-to-room)
            val qualifiedMinutes = segments.filter { it >= 5 }.sum()

            val walked = qualifiedMinutes >= 30
            Log.d("FitSync", "🚶 Segments: $segments, qualified(>=5min): ${qualifiedMinutes}min, walked=$walked")
            walked
        } catch (e: Exception) {
            Log.e("FitSync", "Error in step check: ${e.message}")
            false
        }
    }

    @Deprecated("Use checkDailyWalk() instead")
    suspend fun checkWalkingGoal(date: LocalDate): Boolean {
        return checkDailyWalk(date).isWalked
    }
}