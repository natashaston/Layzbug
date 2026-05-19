package com.layzbug.app.data

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.SessionReadRequest
import com.layzbug.app.WalkSegment
import com.layzbug.app.data.auth.AuthManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class FitSession(
    val date: LocalDate,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long,
    val distanceKm: Double,
    val stepCount: Long,
    val stepsPerMinute: Long,
    val isQualified: Boolean,
    val rejectReason: String?
)

@Singleton
class GoogleFitBackfillManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val installationTracker: InstallationTracker
) {
    companion object {
        private const val TAG = "GoogleFitBackfill"

        // Minimum session duration to qualify as an intentional walk
        private const val MIN_SESSION_MINUTES = 5L

        // Minimum steps per minute for a session to count
        // Based on calibration: genuine walks are 55+ spm, noise is below
        private const val MIN_SESSION_SPM = 55L
    }

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    fun hasFitnessAccess(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, *fitnessOptions.impliedScopes.toTypedArray())
            || authManager.hasFitnessPermission()
    }

    /**
     * Fetches all walking sessions from Google Fit from the sync start date to today.
     * Returns a list of FitSession objects, one per detected walking session.
     * Sessions under 5 minutes or below 55spm are included but marked as not qualified
     * so the UI can show them as strikethrough.
     */
    suspend fun fetchHistoricalWalkingSessions(
        fromDate: LocalDate = installationTracker.getSyncStartDate(),
        toDate: LocalDate = LocalDate.now()
    ): List<FitSession> {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            Log.w(TAG, "No signed-in account — cannot fetch Google Fit history")
            return emptyList()
        }

        if (!hasFitnessAccess()) {
            Log.w(TAG, "Fitness permissions not granted")
            return emptyList()
        }

        return try {
            val zoneId   = ZoneId.systemDefault()
            val startMs  = fromDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMs    = toDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            val request = SessionReadRequest.Builder()
                .readSessionsFromAllApps()
                .includeSleepSessions()
                .read(DataType.TYPE_ACTIVITY_SEGMENT)
                .read(DataType.TYPE_DISTANCE_DELTA)
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .setTimeInterval(startMs, endMs, TimeUnit.MILLISECONDS)
                .build()

            val sessionsClient = Fitness.getSessionsClient(context, account)
            val response = sessionsClient.readSession(request).await()

            Log.d(TAG, "📥 Google Fit returned ${response.sessions.size} sessions")

            val fitSessions = mutableListOf<FitSession>()

            for (session in response.sessions) {
                // Only process walking sessions
                val activityType = session.activity
                if (activityType != "walking" && activityType != "on_foot") {
                    continue
                }

                val startInstant = Instant.ofEpochMilli(
                    session.getStartTime(TimeUnit.MILLISECONDS)
                )
                val endInstant = Instant.ofEpochMilli(
                    session.getEndTime(TimeUnit.MILLISECONDS)
                )
                val durationMinutes = java.time.Duration.between(startInstant, endInstant).toMinutes()
                val sessionDate = startInstant.atZone(zoneId).toLocalDate()

                // Extract steps and distance from data sets
                var totalSteps = 0L
                var totalDistanceMeters = 0.0

                val dataSets = response.getDataSet(session)
                for (dataSet in dataSets) {
                    for (point in dataSet.dataPoints) {
                        when (point.dataType) {
                            DataType.TYPE_STEP_COUNT_DELTA -> {
                                totalSteps += point.getValue(Field.FIELD_STEPS).asInt().toLong()
                            }
                            DataType.TYPE_DISTANCE_DELTA -> {
                                totalDistanceMeters += point.getValue(Field.FIELD_DISTANCE).asFloat()
                            }
                            else -> {}
                        }
                    }
                }

                val distanceKm = Math.round(totalDistanceMeters / 1000.0 * 100.0) / 100.0
                val stepsPerMinute = if (durationMinutes > 0) totalSteps / durationMinutes else 0L

                val (isQualified, rejectReason) = when {
                    durationMinutes < MIN_SESSION_MINUTES ->
                        false to "< 5 mins"
                    stepsPerMinute < MIN_SESSION_SPM ->
                        false to "Low step density"
                    else ->
                        true to null
                }

                fitSessions.add(
                    FitSession(
                        date            = sessionDate,
                        startTime       = startInstant,
                        endTime         = endInstant,
                        durationMinutes = durationMinutes,
                        distanceKm      = distanceKm,
                        stepCount       = totalSteps,
                        stepsPerMinute  = stepsPerMinute,
                        isQualified     = isQualified,
                        rejectReason    = rejectReason
                    )
                )

                Log.d(TAG, "  📍 ${sessionDate}: ${durationMinutes}min | ${totalSteps}steps | ${stepsPerMinute}spm | ${distanceKm}km | qualified=$isQualified")
            }

            Log.d(TAG, "✅ Processed ${fitSessions.size} walking sessions from Google Fit")
            fitSessions

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch Google Fit sessions: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Converts a list of FitSession objects to WalkSegment objects for UI display.
     * Groups sessions by date for use in CalendarDayModel.
     */
    fun toWalkSegments(sessions: List<FitSession>): Map<LocalDate, List<WalkSegment>> {
        return sessions.groupBy { it.date }.mapValues { (_, daySessions) ->
            daySessions.map { session ->
                WalkSegment(
                    durationMinutes = session.durationMinutes,
                    isQualified     = session.isQualified,
                    rejectReason    = session.rejectReason,
                    stepCount       = session.stepCount,
                    stepsPerMinute  = session.stepsPerMinute
                )
            }
        }
    }
}
