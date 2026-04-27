package com.layzbug.app.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    private val NOTIF_MINUTE  = intPreferencesKey("notif_minute")

    val isEnabled: Flow<Boolean> = context.notifDataStore.data
        .map { it[NOTIF_ENABLED] ?: true }

    val notifHour: Flow<Int> = context.notifDataStore.data
        .map { it[NOTIF_HOUR] ?: 18 }

    val notifMinute: Flow<Int> = context.notifDataStore.data
        .map { it[NOTIF_MINUTE] ?: 0 }

    suspend fun setEnabled(enabled: Boolean, hour: Int, minute: Int = 0) {
        context.notifDataStore.edit { prefs ->
            prefs[NOTIF_ENABLED] = enabled
        }
        AlarmScheduler.savePrefsBackup(context, enabled, hour, minute)
        if (enabled) {
            AlarmScheduler.schedule(context, hour, minute)
        } else {
            AlarmScheduler.cancel(context)
        }
    }

    suspend fun setHour(hour: Int) {
        context.notifDataStore.edit { prefs ->
            prefs[NOTIF_HOUR] = hour
        }
        AlarmScheduler.savePrefsBackup(context, true, hour, 0)
        AlarmScheduler.cancel(context)
        AlarmScheduler.schedule(context, hour, 0)
    }

    suspend fun setHourAndMinute(hour: Int, minute: Int) {
        context.notifDataStore.edit { prefs ->
            prefs[NOTIF_HOUR]   = hour
            prefs[NOTIF_MINUTE] = minute
        }
        AlarmScheduler.savePrefsBackup(context, true, hour, minute)
        AlarmScheduler.cancel(context)
        AlarmScheduler.schedule(context, hour, minute)
    }
}