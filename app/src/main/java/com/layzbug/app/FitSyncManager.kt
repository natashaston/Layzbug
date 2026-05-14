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

    /**
     * Walk detection rules:
     * - Each walk segment must be >= 5 minutes
     * - Sum of qualified segments must reach >= 30 minutes
     *
     * Strategy:
     * 1. Read exercise sessions. Compute qualified session minutes (segments >= 5 min).
     * 2. If sessions already qualify (>= 30 min), done.
     * 3. If sessions don't qualify — whether empty OR present but too short/noisy —
     *    ALSO run step inference and take the better of the two readings.
     *
     * FIX: Previously step inference was gated on `sessionResponse.records.isEmpty()`.
     * A single noise session (e.g. a 3-min warm-up) would block inference entirely,
     * leaving a valid 40-min walk marked as unwalked.
     *
     * We take maxOf(session, inferred) rather than summing — both sensors describe
     * the same physical walk, so adding them would double-count.
     */
    suspend fun checkDailyWalk(date: LocalDate): DailyWalkResult {
        try {
            val zoneId    = ZoneId.systemDefault()
            val startTime = date.atStartOfDay(zoneId).toInstant()
            val endTime   = date.plusDays(1).atStartOfDay(zoneId).toInstant()
            val timeRange = TimeRangeFilter.between(startTime, endTime)

            // ── Strategy 1: Exercise sessions ───────────────────────────────
            val sessionResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType      = ExerciseSessionRecord::class,
                    timeRangeFilter = timeRange
                )
            )

            val sessionDurations        = sessionResponse.records.map { session ->
                Duration.between(session.startTime, session.endTime).toMinutes()
            }
            val qualifiedSessionMinutes = sessionDurations.filter { it >= 5 }.sum()

            // ── Strategy 2: Step inference — supplement when sessions don't qualify ──
            // Runs whenever qualifiedSessionMinutes < 30, including when sessions
            // exist but are all noise (< 5 min each). Previously only ran when
            // records were completely empty — that was the bug.
            val qualifiedInferredMinutes: Long = if (qualifiedSessionMinutes < 30) {
                inferWalksFromSteps(timeRange).filter { it >= 5 }.sum()
            } else {
                0L
            }

            // Take the better reading — don't add (same walk, two sensors)
            val totalQualifiedMinutes = maxOf(qualifiedSessionMinutes, qualifiedInferredMinutes)
            val isWalked              = totalQualifiedMinutes >= 30

            // ── Distance ────────────────────────────────────────────────────
            val distanceKm = try {
                val aggregateResponse = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics         = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = timeRange
                    )
                )
                val distanceMeters = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                Math.round(distanceMeters / 1000.0 * 100.0) / 100.0
            } catch (e: Exception) {
                Log.e("FitSync", "Error reading distance for $date: ${e.message}")
                0.0
            }

            Log.d(
                "FitSync",
                "📊 $date: sessions=$sessionDurations " +
                        "qualifiedSession=${qualifiedSessionMinutes}min " +
                        "qualifiedInferred=${qualifiedInferredMinutes}min " +
                        "total=${totalQualifiedMinutes}min walked=$isWalked ${distanceKm}km"
            )

            return DailyWalkResult(
                isWalked     = isWalked,
                distanceKm   = distanceKm,
                totalMinutes = totalQualifiedMinutes,
                sessionCount = sessionResponse.records.size
            )
        } catch (e: Exception) {
            Log.e("FitSync", "Error for $date: ${e.message}", e)
            return DailyWalkResult(isWalked = false, distanceKm = 0.0)
        }
    }

    /**
     * Infer walking segments from step data.
     * Reads steps in 1-minute buckets; treats >= 40 steps/min as walking.
     * Returns duration (in minutes) of each continuous walking segment.
     * No gap tolerance — any minute below 40 steps ends the segment.
     */
    private suspend fun inferWalksFromSteps(timeRange: TimeRangeFilter): List<Long> {
        return try {
            val response = healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics         = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeRange,
                    timeRangeSlicer = Duration.ofMinutes(1)
                )
            )

            val segments              = mutableListOf<Long>()
            var currentSegmentMinutes = 0L

            for (bucket in response) {
                val stepsInMinute = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
                if (stepsInMinute >= 40) {
                    currentSegmentMinutes++
                } else {
                    if (currentSegmentMinutes > 0) segments.add(currentSegmentMinutes)
                    currentSegmentMinutes = 0
                }
            }
            if (currentSegmentMinutes > 0) segments.add(currentSegmentMinutes)

            segments
        } catch (e: Exception) {
            Log.e("FitSync", "Error inferring walks from steps: ${e.message}")
            emptyList()
        }
    }

    @Deprecated("Use checkDailyWalk() instead")
    suspend fun checkWalkingGoal(date: LocalDate): Boolean {
        return checkDailyWalk(date).isWalked
    }
}