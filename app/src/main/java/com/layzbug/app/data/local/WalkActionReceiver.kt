package com.layzbug.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.layzbug.app.data.repository.WalkRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class WalkActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var walkRepository: WalkRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHelper.ACTION_I_WALKED -> {
                Log.d("WalkActionReceiver", "User tapped 'I walked today'")
                // Mark today as walked manually
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        walkRepository.updateManualWalk(LocalDate.now(), true)
                        Log.d("WalkActionReceiver", "✅ Today marked as walked")
                    } catch (e: Exception) {
                        Log.e("WalkActionReceiver", "Failed: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
                // Dismiss notification
                NotificationManagerCompat.from(context)
                    .cancel(NotificationHelper.NOTIFICATION_ID)
            }

            NotificationHelper.ACTION_DISMISS -> {
                Log.d("WalkActionReceiver", "User dismissed notification")
                NotificationManagerCompat.from(context)
                    .cancel(NotificationHelper.NOTIFICATION_ID)
            }
        }
    }
}
