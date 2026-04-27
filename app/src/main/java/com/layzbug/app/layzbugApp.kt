package com.layzbug.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.layzbug.app.notifications.AlarmScheduler
import com.layzbug.app.notifications.NotificationHelper
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
        // Schedule daily alarm at 18:00 — AlarmScheduler uses KEEP logic
        // (only schedules if not already pending)
        AlarmScheduler.schedule(this, hourOfDay = 18)
    }
}