package com.layzbug.app.data.local

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.layzbug.app.data.repository.WalkRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class WalkTransitionReceiver : BroadcastReceiver() {

    @Inject lateinit var walkRepository: WalkRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return

        val prefs = context.getSharedPreferences("layzbug_install", Context.MODE_PRIVATE)

        for (event in result.transitionEvents) {
            if (event.activityType == DetectedActivity.WALKING) {
                when (event.transitionType) {
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                        val startTime = System.currentTimeMillis()
                        prefs.edit().putLong("walk_start_time", startTime).apply()
                        Log.d("WalkTransition", "🚶 System ML Engine Event: ENTERED intentional walk window at $startTime")
                    }
                    ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                        val startTime = prefs.getLong("walk_start_time", 0L)
                        if (startTime == 0L) continue

                        val endTime = System.currentTimeMillis()
                        val durationMinutes = (endTime - startTime) / 60000L
                        prefs.edit().remove("walk_start_time").apply()

                        Log.d("WalkTransition", "🛑 System ML Engine Event: EXITED intentional walk window. Duration: $durationMinutes mins")

                        if (durationMinutes >= 5) {
                            receiverScope.launch {
                                val date = LocalDate.now()

                                // Since Google's Activity Recognition API confirmed this exact window
                                // as an intentional continuous walk, we record it directly as a clean segment.
                                walkRepository.updateWalkFromStepDetector(
                                    date = date,
                                    startTime = Instant.ofEpochMilli(startTime),
                                    endTime = Instant.ofEpochMilli(endTime),
                                    durationMinutes = durationMinutes,
                                    stepCount = 0L, // Handled automatically during total daily aggregation passes
                                    stepsPerMinute = 0L,
                                    isQualified = true,
                                    rejectReason = null
                                )
                                Log.d("WalkTransition", "✅ Verified live ML session saved directly to repository timeline: $durationMinutes mins")
                            }
                        }
                    }
                }
            }
        }
    }
}