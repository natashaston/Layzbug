package com.layzbug.app

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
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
    private val DAILY_GOAL_MINUTES = 30L
    private val MIN_SESSION_MINS = 5L
    private val MIN_SESSION_SPM = 55L
    private val MAX_GAP_TOLERANCE_MS = 2 * 60 * 1000L // 2 minutes

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

            val segments = readExerciseSessions(timeRange).let { exerciseSegments ->
                if (exerciseSegments.isNotEmpty()) {
                    Log.d("FitSync", "📊 $date: using ExerciseSessionRecord (${exerciseSegments.size} sessions)")
                    exerciseSegments
                } else {
                    Log.d("FitSync", "📊 $date: parsing raw record timelines")
                    inferWalksFromRawSteps(timeRange)
                }
            }

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

    private suspend fun readExerciseSessions(timeRange: TimeRangeFilter): List<WalkSegment> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType      = ExerciseSessionRecord::class,
                    timeRangeFilter = timeRange
                )
            )

            val exerciseRecords = response.records.filter { record ->
                record.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_WALKING || record.exerciseType == 80
            }

            if (exerciseRecords.isEmpty()) return emptyList()

            val stepsResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(recordType = StepsRecord::class, timeRangeFilter = timeRange)
            )
            val stepRecords = stepsResponse.records

            exerciseRecords.map { record ->
                val startMs = record.startTime.toEpochMilli()
                val endMs = record.endTime.toEpochMilli()
                val durationMins = (endMs - startMs) / 60_000L

                val sessionSteps = stepRecords.filter {
                    it.startTime.toEpochMilli() >= startMs && it.endTime.toEpochMilli() <= endMs
                }.sumOf { it.count }

                val stepsPerMin = if (durationMins > 0) sessionSteps / durationMins else 0L

                val (isQualified, rejectReason) = when {
                    durationMins < MIN_SESSION_MINS -> false to "< $MIN_SESSION_MINS mins"
                    stepsPerMin < MIN_SESSION_SPM -> false to "Low step density ($stepsPerMin spm)"
                    else -> true to null
                }

                WalkSegment(
                    durationMinutes = durationMins,
                    isQualified = isQualified,
                    rejectReason = rejectReason,
                    stepCount = sessionSteps,
                    stepsPerMinute = stepsPerMin
                )
            }
        } catch (e: Exception) {
            Log.e("FitSync", "Error reading ExerciseSessionRecords: ${e.message}")
            emptyList()
        }
    }

    private suspend fun inferWalksFromRawSteps(timeRange: TimeRangeFilter): List<WalkSegment> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(recordType = StepsRecord::class, timeRangeFilter = timeRange)
            )

            val records = response.records.sortedBy { it.startTime }
            if (records.isEmpty()) return emptyList()

            val segments = mutableListOf<WalkSegment>()

            var activeSessionStart = 0L
            var activeSessionEnd = 0L
            var activeSessionSteps = 0L

            fun flushActiveSession() {
                if (activeSessionStart == 0L) return
                val durationMins = (activeSessionEnd - activeSessionStart) / 60_000L

                if (durationMins > 0) {
                    val stepsPerMin = activeSessionSteps / durationMins
                    val segment = when {
                        durationMins < MIN_SESSION_MINS -> WalkSegment(durationMins, false, "< $MIN_SESSION_MINS mins", activeSessionSteps, stepsPerMin)
                        stepsPerMin < MIN_SESSION_SPM -> WalkSegment(durationMins, false, "Low step density ($stepsPerMin spm)", activeSessionSteps, stepsPerMin)
                        else -> WalkSegment(durationMins, true, null, activeSessionSteps, stepsPerMin)
                    }
                    segments.add(segment)
                }

                activeSessionStart = 0L
                activeSessionEnd = 0L
                activeSessionSteps = 0L
            }

            for (record in records) {
                val recordStart = record.startTime.toEpochMilli()
                val recordEnd = record.endTime.toEpochMilli()
                val recordDurationMins = maxOf(1L, (recordEnd - recordStart) / 60_000L)
                val recordSpm = record.count / recordDurationMins

                // High-velocity filter: If the individual piece of data is too slow,
                // it cannot be part of an intentional walking session.
                if (recordSpm < MIN_SESSION_SPM) {
                    // Close any active session right before this slow block
                    flushActiveSession()
                    continue
                }

                if (activeSessionStart == 0L) {
                    // Start a fresh intentional session
                    activeSessionStart = recordStart
                    activeSessionEnd = recordEnd
                    activeSessionSteps = record.count
                } else {
                    // We are in an active session. Check if this next fast chunk is close enough.
                    if ((recordStart - activeSessionEnd) <= MAX_GAP_TOLERANCE_MS) {
                        activeSessionEnd = maxOf(activeSessionEnd, recordEnd)
                        activeSessionSteps += record.count
                    } else {
                        // Too much time passed since the last fast segment. Close it.
                        flushActiveSession()
                        // Start a new session with this fast chunk
                        activeSessionStart = recordStart
                        activeSessionEnd = recordEnd
                        activeSessionSteps = record.count
                    }
                }
            }

            // Flush out the remaining active session if one exists at the end of the day
            flushActiveSession()

            segments
        } catch (e: Exception) {
            Log.e("FitSync", "Error in velocity step parsing: ${e.message}")
            emptyList()
        }
    }
}