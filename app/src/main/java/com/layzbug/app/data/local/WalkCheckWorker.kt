package com.layzbug.app.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.layzbug.app.data.repository.WalkRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import java.time.LocalTime
import java.time.ZoneId

@HiltWorker
class WalkCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val walkRepository: WalkRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val today   = LocalDate.now()
            val isWalked = walkRepository.getWalkStatus(today)
            Log.d("WalkCheckWorker", "Today: $today, walked: $isWalked")

            if (!isWalked) {
                NotificationHelper.showWalkReminder(applicationContext)
                Log.d("WalkCheckWorker", "🔔 Walk reminder sent")
            } else {
                Log.d("WalkCheckWorker", "✅ Already walked — no notification")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("WalkCheckWorker", "Worker failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "layzbug_walk_check"

        /**
         * Schedules a daily check at the given hour (24h).
         * Uses PeriodicWorkRequest with a flex window so Android
         * can run it within ±30 min of the target hour to save battery.
         */
        fun schedule(context: Context, hourOfDay: Int = 18) {
            // Calculate initial delay to next occurrence of hourOfDay
            val now        = LocalTime.now()
            val target     = LocalTime.of(hourOfDay, 0)
            val delayMins  = if (now.isBefore(target)) {
                now.until(target, java.time.temporal.ChronoUnit.MINUTES)
            } else {
                // Already past today's target — schedule for tomorrow
                now.until(target.plusHours(24), java.time.temporal.ChronoUnit.MINUTES)
            }

            val request = PeriodicWorkRequestBuilder<WalkCheckWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 30,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setInitialDelay(delayMins, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // don't reset if already scheduled
                request
            )

            Log.d("WalkCheckWorker", "Scheduled daily check at ${hourOfDay}:00 (delay: ${delayMins}min)")
        }
    }
}
