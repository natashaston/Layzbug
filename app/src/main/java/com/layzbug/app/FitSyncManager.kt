package com.layzbug.app

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WalkSegment(
    val durationMinutes: Long,
    val isQualified: Boolean,
    val rejectReason: String? = null,
    val stepCount: Long = 0L,
    val stepsPerMinute: Long = 0L
)

data class DailyWalkResult(
    val isWalked: Boolean,
    val distanceKm: Double,
    val totalMinutes: Long = 0,
    val sessionCount: Int = 0,
    val allSegments: List<WalkSegment> = emptyList()
)

@Singleton
class FitSyncManager @Inject constructor(
    private val healthConnectClient: HealthConnectClient
) {
    // A day is walked when the sum of all qualified segments reaches this.
    private val DAILY_GOAL_MINUTES = 30L

    // Minimum step cadence for a record to be considered intentional walking.
    // Filters out slow shuffling, standing fidgets, and household movement.
    private val MIN_SESSION_SPM = 55L

    // Maximum gap between two fast-step records before they are treated as
    // separate sessions. 60 seconds covers a traffic light or brief pause
    // without stitching unrelated walks together.
    private val MAX_GAP_TOLERANCE_MS = 60 * 1000L

    // Minimum duration for a merged session to qualify.
    // Filters out elevator rides, short bursts between shelves, bathroom trips —
    // anything that isn't sustained intentional walking.
    private val MIN_SEGMENT_MINUTES = 5L

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
        return try {
            val zoneId    = ZoneId.systemDefault()
            val startTime = date.atStartOfDay(zoneId).toInstant()
            val endTime   = date.plusDays(1).atStartOfDay(zoneId).toInstant()
            val timeRange = TimeRangeFilter.between(startTime, endTime)

            val segments  = inferWalksFromRawSteps(timeRange)

            val totalQualifiedMinutes = segments.filter { it.isQualified }.sumOf { it.durationMinutes }
            val isWalked = totalQualifiedMinutes >= DAILY_GOAL_MINUTES

            val distanceKm = try {
                val aggregateResponse = healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics         = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = timeRange
                    )
                )
                val meters = aggregateResponse[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                Math.round(meters / 1000.0 * 100.0) / 100.0
            } catch (e: Exception) {
                Log.e("FitSync", "Error reading distance for $date: ${e.message}")
                0.0
            }

            DailyWalkResult(
                isWalked     = isWalked,
                distanceKm   = distanceKm,
                totalMinutes = totalQualifiedMinutes,
                sessionCount = segments.count { it.isQualified },
                allSegments  = segments
            )
        } catch (e: Exception) {
            Log.e("FitSync", "Error for $date: ${e.message}", e)
            DailyWalkResult(isWalked = false, distanceKm = 0.0)
        }
    }

    private suspend fun inferWalksFromRawSteps(timeRange: TimeRangeFilter): List<WalkSegment> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(recordType = StepsRecord::class, timeRangeFilter = timeRange)
            )

            val rawRecords = response.records.sortedBy { it.startTime }

            if (rawRecords.isEmpty()) return emptyList()

            // Deduplicate StepsRecords from multiple HC sources writing to the same time windows.
            // Health Connect returns records from all registered sources (Google Fit, OEM sensor hub,
            // Android health platform) without deduplication. Patterns handled:
            //   1. Exact duplicate — same start time + same step count
            //   2. Fully contained — candidate sits entirely inside an existing record's window
            //   3. Same end time, later start — partial overlap from a second source
            //   4. Existing fully inside candidate — candidate is larger, replace existing
            val records = mutableListOf<StepsRecord>()
            for (candidate in rawRecords) {
                var isDuplicate  = false
                var replaceIndex = -1
                for ((index, existing) in records.withIndex()) {
                    val exactDup          = candidate.startTime == existing.startTime && candidate.count == existing.count
                    val candInExist       = candidate.startTime >= existing.startTime && candidate.endTime <= existing.endTime
                    val sameEndLaterStart = candidate.endTime == existing.endTime && candidate.startTime > existing.startTime
                    val existInCand       = existing.startTime >= candidate.startTime && existing.endTime <= candidate.endTime
                    when {
                        exactDup || candInExist || sameEndLaterStart -> { isDuplicate = true; break }
                        existInCand -> { isDuplicate = true; replaceIndex = index; break }
                    }
                }
                when {
                    !isDuplicate      -> records.add(candidate)
                    replaceIndex >= 0 -> records[replaceIndex] = candidate
                }
            }

            val segments = mutableListOf<WalkSegment>()
            var activeSessionStart = 0L
            var activeSessionEnd   = 0L
            var activeSessionSteps = 0L

            fun flushActiveSession() {
                if (activeSessionStart == 0L) return
                val durationMins = (activeSessionEnd - activeSessionStart) / 60_000L
                if (durationMins > 0) {
                    val stepsPerMin = activeSessionSteps / durationMins
                    segments.add(when {
                        durationMins < MIN_SEGMENT_MINUTES ->
                            WalkSegment(durationMins, false, "Too short (${durationMins}min < ${MIN_SEGMENT_MINUTES}min)", activeSessionSteps, stepsPerMin)
                        stepsPerMin < MIN_SESSION_SPM ->
                            WalkSegment(durationMins, false, "Low step density ($stepsPerMin spm)", activeSessionSteps, stepsPerMin)
                        else ->
                            WalkSegment(durationMins, true, null, activeSessionSteps, stepsPerMin)
                    })
                }
                activeSessionStart = 0L
                activeSessionEnd   = 0L
                activeSessionSteps = 0L
            }

            for (record in records) {
                val recordStart   = record.startTime.toEpochMilli()
                val recordEnd     = record.endTime.toEpochMilli()
                val recordDurMins = maxOf(1L, (recordEnd - recordStart) / 60_000L)
                val recordSpm     = record.count / recordDurMins

                // Pre-filter: if this individual record is too slow to be walking,
                // flush any active session and skip it.
                if (recordSpm < MIN_SESSION_SPM) {
                    flushActiveSession()
                    continue
                }

                if (activeSessionStart == 0L) {
                    activeSessionStart = recordStart
                    activeSessionEnd   = recordEnd
                    activeSessionSteps = record.count
                } else {
                    val gapMs = recordStart - activeSessionEnd
                    if (gapMs <= MAX_GAP_TOLERANCE_MS) {
                        activeSessionEnd    = maxOf(activeSessionEnd, recordEnd)
                        activeSessionSteps += record.count
                    } else {
                        flushActiveSession()
                        activeSessionStart = recordStart
                        activeSessionEnd   = recordEnd
                        activeSessionSteps = record.count
                    }
                }
            }

            flushActiveSession()
            segments

        } catch (e: Exception) {
            Log.e("FitSync", "Error in raw step inference: ${e.message}")
            emptyList()
        }
    }
}