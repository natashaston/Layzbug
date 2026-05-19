package com.layzbug.app.data.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.layzbug.app.MainActivity
import com.layzbug.app.R
import com.layzbug.app.WalkSegment
import com.layzbug.app.data.repository.AutoWalkSession
import com.layzbug.app.data.repository.WalkRepository
import com.layzbug.app.data.repository.SupabaseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class StepDetectorService : Service(), SensorEventListener {

    @Inject lateinit var walkRepository: WalkRepository
    @Inject lateinit var supabaseRepository: SupabaseRepository
    @Inject lateinit var activityTransitionManager: ActivityTransitionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var stepDetectorSensor: Sensor? = null

    private var sessionStartTime: Instant? = null
    private var lastStepTime: Instant? = null
    private var sessionStepCount: Long = 0
    private var sessionEndJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "StepDetectorService"
        private const val GAP_TIMEOUT_MS = 2 * 60 * 1000L  // 2 minutes
        private const val MIN_SESSION_MINUTES = 5L
        private const val MIN_SESSION_SPM = 55L

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "step_detector_channel"

        const val ACTION_START = "com.layzbug.app.START_STEP_DETECTOR"
        const val ACTION_STOP  = "com.layzbug.app.STOP_STEP_DETECTOR"

        fun start(context: Context) {
            val intent = Intent(context, StepDetectorService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StepDetectorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepDetectorSensor == null) {
            Log.w(TAG, "⚠️ TYPE_STEP_DETECTOR not available — falling back to TYPE_STEP_COUNTER")
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        }

        createNotificationChannel()
        Log.d(TAG, "✅ StepDetectorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "🛑 Stop requested")
                flushCurrentSession()
                activityTransitionManager.unregisterTransitions()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                Log.d(TAG, "🚀 Starting step detector and binding Activity Transitions")
                startForeground(NOTIFICATION_ID, buildNotification())
                registerStepSensor()

                // Spin up hardware Activity Recognition loops concurrently to lock timeline boundaries
                activityTransitionManager.registerTransitions()
            }
        }
        return START_STICKY
    }

    private fun registerStepSensor() {
        val sensor = stepDetectorSensor
        if (sensor == null) {
            Log.e(TAG, "❌ No step sensor available")
            return
        }

        val registered = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        Log.d(TAG, if (registered) "✅ Step sensor registered" else "❌ Failed to register step sensor")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_DETECTOR &&
            event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val now = Instant.now()

        if (sessionStartTime == null) {
            sessionStartTime = now
            sessionStepCount = 0
            Log.d(TAG, "🚶 New tracking session started at $now")
        }

        sessionStepCount++
        lastStepTime = now

        sessionEndJob?.cancel()
        sessionEndJob = serviceScope.launch {
            delay(GAP_TIMEOUT_MS)
            flushCurrentSession()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun flushCurrentSession() {
        val start = sessionStartTime ?: return
        val end   = lastStepTime ?: return
        val steps = sessionStepCount

        sessionStartTime = null
        lastStepTime = null
        sessionStepCount = 0
        sessionEndJob?.cancel()

        val durationMs      = end.toEpochMilli() - start.toEpochMilli()
        val durationMinutes = durationMs / 60_000L
        val stepsPerMinute  = if (durationMinutes > 0) steps / durationMinutes else 0L

        val (isQualified, rejectReason) = when {
            durationMinutes < MIN_SESSION_MINUTES -> false to "< 5 mins"
            stepsPerMinute < MIN_SESSION_SPM -> false to "Low step density"
            else -> true to null
        }

        val date = start.atZone(ZoneId.systemDefault()).toLocalDate()

        Log.d(TAG, "🏁 Session complete: $date | ${durationMinutes}min | ${steps}steps | ${stepsPerMinute}spm | qualified=$isQualified")

        serviceScope.launch {
            saveSession(
                date            = date,
                startTime       = start,
                endTime         = end,
                durationMinutes = durationMinutes,
                stepCount       = steps,
                stepsPerMinute  = stepsPerMinute,
                isQualified     = isQualified,
                rejectReason    = rejectReason
            )
        }
    }

    private suspend fun saveSession(
        date: LocalDate,
        startTime: Instant,
        endTime: Instant,
        durationMinutes: Long,
        stepCount: Long,
        stepsPerMinute: Long,
        isQualified: Boolean,
        rejectReason: String?
    ) {
        walkRepository.updateWalkFromStepDetector(
            date            = date,
            startTime       = startTime,
            endTime         = endTime,
            durationMinutes = durationMinutes,
            stepCount       = stepCount,
            stepsPerMinute  = stepsPerMinute,
            isQualified     = isQualified,
            rejectReason    = rejectReason
        )

        val autoSession = AutoWalkSession(
            userId          = "",
            walkDate        = date.toString(),
            startTime       = startTime.toString(),
            endTime         = endTime.toString(),
            durationMinutes = durationMinutes,
            stepCount       = stepCount,
            stepsPerMinute  = stepsPerMinute,
            isQualified     = isQualified,
            rejectReason    = rejectReason,
            source          = "step_detector"
        )
        supabaseRepository.upsertAutoWalkSessions(date, listOf(autoSession))
        Log.d(TAG, "✅ Local and remote sync tasks complete for processed session.")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Walk Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks your walks in the background"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Layzbug")
            .setContentText("Tracking your walks")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        flushCurrentSession()
        activityTransitionManager.unregisterTransitions()
        Log.d(TAG, "💤 StepDetectorService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}