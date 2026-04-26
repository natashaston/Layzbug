package com.layzbug.app.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notifDataStore by preferencesDataStore(name = "notification_prefs")

@Singleton
class NotificationPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
    private val NOTIF_HOUR    = intPreferencesKey("notif_hour")

    val isEnabled: Flow<Boolean> = context.notifDataStore.data
        .map { it[NOTIF_ENABLED] ?: true }   // default on

    val notifHour: Flow<Int> = context.notifDataStore.data
        .map { it[NOTIF_HOUR] ?: 18 }        // default 18:00

    suspend fun setEnabled(enabled: Boolean, hour: Int) {
        context.notifDataStore.edit { prefs ->
            prefs[NOTIF_ENABLED] = enabled
        }
        if (enabled) {
            WalkCheckWorker.schedule(context, hour)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(WalkCheckWorker.WORK_NAME)
        }
    }

    suspend fun setHour(hour: Int) {
        context.notifDataStore.edit { prefs ->
            prefs[NOTIF_HOUR] = hour
        }
        // Reschedule at new time — cancel existing and create new
        WorkManager.getInstance(context).cancelUniqueWork(WalkCheckWorker.WORK_NAME)
        WalkCheckWorker.schedule(context, hour)
    }
}
