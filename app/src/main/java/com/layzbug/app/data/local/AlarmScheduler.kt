package com.layzbug.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar

object AlarmScheduler {

    private const val ALARM_REQUEST_CODE = 1001

    /**
     * Schedules a daily exact alarm at the given hour.
     * Uses setExactAndAllowWhileIdle so it fires even in doze mode.
     * After firing, the receiver reschedules for the next day.
     */
    fun schedule(context: Context, hourOfDay: Int, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, WalkReminderReceiver::class.java).apply {
                action = WalkReminderReceiver.ACTION_WALK_REMINDER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate next occurrence of hourOfDay
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If we've already passed today's time, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        Log.d("AlarmScheduler", "Scheduling alarm for ${calendar.time}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ — check if we can schedule exact alarms
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    intent
                )
            } else {
                // Fallback to inexact if permission not granted
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    intent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                intent
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, WalkReminderReceiver::class.java).apply {
                action = WalkReminderReceiver.ACTION_WALK_REMINDER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(intent)
        Log.d("AlarmScheduler", "Alarm cancelled")
    }

    /**
     * Called on boot — reads saved hour from DataStore and reschedules.
     */
    fun rescheduleFromPrefs(context: Context) {
        val prefs   = context.getSharedPreferences("notif_prefs_backup", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("notif_enabled", true)
        val hour    = prefs.getInt("notif_hour", 18)
        val minute  = prefs.getInt("notif_minute", 0)
        if (enabled) {
            schedule(context, hour, minute)
            Log.d("AlarmScheduler", "Rescheduled after boot at $hour:$minute")
        }
    }

    fun savePrefsBackup(context: Context, enabled: Boolean, hour: Int, minute: Int = 0) {
        context.getSharedPreferences("notif_prefs_backup", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("notif_enabled", enabled)
            .putInt("notif_hour", hour)
            .putInt("notif_minute", minute)
            .apply()
    }

    /**
     * Called from the receiver after it fires — reschedules for next day.
     */
    fun scheduleNextDay(context: Context, hourOfDay: Int, minute: Int = 0) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, WalkReminderReceiver::class.java).apply {
                action = WalkReminderReceiver.ACTION_WALK_REMINDER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, intent)
        }
        Log.d("AlarmScheduler", "Next alarm scheduled for ${calendar.time}")
    }
}