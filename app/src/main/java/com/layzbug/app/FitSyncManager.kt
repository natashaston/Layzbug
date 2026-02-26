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
    /**
     * Checks if the set of required permissions is already granted.
     */
    suspend fun hasPermissions(permissions: Set<String>): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            Log.e("FitSync", "Error checking permissions: ${e.message}")
            false
        }
    }

    /**
     * Checks daily walking goal and returns both walk status and distance.
     *
     * Detection strategy (same as original, plus distance):
     * 1. Check ExerciseSessionRecords for manually logged workouts >= 30 min
     * 2. Fall back to City-Proof Streak Logic using per-minute step aggregation
     *    - Any minute with 40+ steps counts as "walking"
     *    - Allows up to 4-min gaps (traffic lights, crossings)
     *    - If continuous walking streak >= 30 min -> walked
     * 3. Also aggregate total distance for the day
     */
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
     * City-Proof Streak Logic: Analyzes per-minute step data to find
     * sustained walking periods. Tolerates brief stops (traffic lights,
     * crossings) up to 4 minutes.
     *
     * A minute with 40+ steps is considered "walking".
     * If a continuous walking streak (with gap tolerance) reaches 30 min,
     * returns true.
     */
    private suspend fun checkStepStreak(timeRange: TimeRangeFilter): Boolean {
        return try {
            val response: List<*>? = healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeRange,
                    timeRangeSlicer = Duration.ofMinutes(1)
                )
            )

            @Suppress("SENSELESS_COMPARISON")
            if (response == null || response.isEmpty()) return false

            var continuousMinutes = 0
            var gapMinutes = 0
            val maxAllowedGap = 4

            for (bucket in response) {
                if (bucket == null) continue
                val stepsInMinute = try {
                    (bucket as? androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration)
                        ?.result?.get(StepsRecord.COUNT_TOTAL) ?: 0L
                } catch (e: Exception) {
                    0L
                }

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

            false
        } catch (e: Exception) {
            Log.e("FitSync", "Error in step streak check: ${e.message}")
            false
        }
    }

    /**
     * @deprecated Use checkDailyWalk() instead which returns both walk status and distance.
     */
    suspend fun checkWalkingGoal(date: LocalDate): Boolean {
        return checkDailyWalk(date).isWalked
    }
}