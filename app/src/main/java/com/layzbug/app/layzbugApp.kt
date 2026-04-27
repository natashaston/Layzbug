package com.layzbug.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.layzbug.app.notifications.NotificationHelper
import com.layzbug.app.notifications.WalkCheckWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LayzbugApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        WalkCheckWorker.schedule(this, hourOfDay = 18)
        requestBatteryOptimisationExemption()
    }

    /**
     * Asks Android to exclude Layzbug from battery optimisation.
     * Without this, WorkManager periodic tasks are killed on many devices
     * (especially Motorola, Xiaomi, OnePlus) when the phone is not charging
     * or the app is in the background.
     *
     * This opens the system dialog once — user taps "Allow" and it's done.
     * Safe to call on every launch — the system only shows the dialog if
     * the app is not already exempt.
     */
    private fun requestBatteryOptimisationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }
}