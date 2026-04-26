package com.layzbug.app

import android.app.Application
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

        // Create notification channel (safe to call multiple times)
        NotificationHelper.createChannel(this)

        // Schedule daily walk check at 18:00
        WalkCheckWorker.schedule(this, hourOfDay = 18)
    }
}