package com.layzbug.app.data.local

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityTransitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = ActivityRecognition.getClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, WalkTransitionReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("InlinedApi")
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun registerTransitions() {
        if (!hasPermission()) {
            Log.w("ActivityTransition", "Permission missing, cannot register.")
            return
        }

        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)

        client.requestActivityTransitionUpdates(request, pendingIntent)
            .addOnSuccessListener {
                Log.d("ActivityTransition", "✅ Successfully registered for walking transitions")
            }
            .addOnFailureListener { e ->
                Log.e("ActivityTransition", "❌ Failed to register transitions: ${e.message}")
            }
    }

    @SuppressLint("MissingPermission")
    fun unregisterTransitions() {
        if (!hasPermission()) return
        client.removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener {
                Log.d("ActivityTransition", "✅ Successfully unregistered transitions")
            }
            .addOnFailureListener { e ->
                Log.e("ActivityTransition", "❌ Failed to unregister transitions: ${e.message}")
            }
    }
}