package com.layzbug.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.layzbug.app.R
import com.layzbug.app.MainActivity

object NotificationHelper {

    const val CHANNEL_ID       = "layzbug_walk_reminder"
    const val NOTIFICATION_ID  = 1001
    const val ACTION_I_WALKED  = "com.layzbug.app.ACTION_I_WALKED"
    const val ACTION_DISMISS   = "com.layzbug.app.ACTION_DISMISS"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Walk Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily reminder if you haven't walked for 30 minutes"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showWalkReminder(context: Context) {
        // Tap notification → open app
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "I walked today" → WalkActionReceiver
        val walkedIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(ACTION_I_WALKED).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "Ok" → just dismisses
        val dismissIntent = PendingIntent.getBroadcast(
            context, 2,
            Intent(ACTION_DISMISS).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_layzbug)
            .setContentTitle("No walk detected today.")
            .setContentText("Get your ass moving! 🍑")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .addAction(0, "I walked today", walkedIntent)
            .addAction(0, "Ok", dismissIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
