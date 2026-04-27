package com.layzbug.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.layzbug.app.data.local.WalkDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import javax.inject.Inject

/**
 * Fires at the scheduled time via AlarmManager.
 * Checks if today is walked — if not, shows the notification.
 * This replaces WorkManager for time-critical delivery.
 */
@AndroidEntryPoint
class WalkReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var walkDao: WalkDao

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WALK_REMINDER -> {
                Log.d("WalkReminderReceiver", "⏰ Alarm fired — checking walk status")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val today = LocalDate.now()
                        val isWalked = walkDao.getWalkStatus(today) ?: false
                        Log.d("WalkReminderReceiver", "Today: $today, walked: $isWalked")
                        if (!isWalked) {
                            NotificationHelper.showWalkReminder(context)
                            Log.d("WalkReminderReceiver", "🔔 Notification sent")
                        } else {
                            Log.d("WalkReminderReceiver", "✅ Already walked — skipping")
                        }
                        // Always reschedule for next day using SharedPreferences backup
                        val prefs  = context.getSharedPreferences("notif_prefs_backup", Context.MODE_PRIVATE)
                        val hour   = prefs.getInt("notif_hour", 18)
                        val minute = prefs.getInt("notif_minute", 0)
                        AlarmScheduler.scheduleNextDay(context, hour, minute)
                    } catch (e: Exception) {
                        Log.e("WalkReminderReceiver", "Error: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            // Reschedule after device reboot
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("WalkReminderReceiver", "📱 Boot completed — rescheduling alarm")
                AlarmScheduler.rescheduleFromPrefs(context)
            }
        }
    }

    companion object {
        const val ACTION_WALK_REMINDER = "com.layzbug.app.ACTION_WALK_REMINDER"
    }
}