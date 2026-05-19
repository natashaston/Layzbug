package com.layzbug.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallationTracker @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences("layzbug_install", Context.MODE_PRIVATE)

    init {
        val onboardingComplete = prefs.getBoolean("onboarding_complete", false)
        val freshInstallMarker = prefs.getBoolean("fresh_install_marker", false)

        if (onboardingComplete && !freshInstallMarker) {
            prefs.edit()
                .remove("onboarding_complete")
                .apply()
        }
    }

    // ── Onboarding ───────────────────────────────────────────────────

    fun isOnboardingComplete(): Boolean =
        prefs.getBoolean("onboarding_complete", false)

    fun setOnboardingComplete() {
        prefs.edit()
            .putBoolean("onboarding_complete", true)
            .putBoolean("fresh_install_marker", true)
            .apply()
    }

    // ── Hardware Walk Tracking ────────────────────────────────────────

    fun setWalkStartTime(timeMs: Long) =
        prefs.edit().putLong("walk_start_time", timeMs).apply()

    fun getWalkStartTime(): Long =
        prefs.getLong("walk_start_time", 0L)

    fun clearWalkStartTime() =
        prefs.edit().remove("walk_start_time").apply()

    // ── Sync start date ───────────────────────────────────────────────

    fun getSyncStartDate(): LocalDate {
        val saved = prefs.getString("sync_start_date", null)
        return if (saved != null) {
            LocalDate.parse(saved)
        } else {
            val startOfYear = LocalDate.of(LocalDate.now().year, 1, 1)
            prefs.edit().putString("sync_start_date", startOfYear.toString()).apply()
            startOfYear
        }
    }

    // ── Initial full sync flag ────────────────────────────────────────

    fun hasInitialSyncDone(): Boolean =
        prefs.getBoolean("initial_sync_done", false)

    fun markInitialSyncDone() =
        prefs.edit().putBoolean("initial_sync_done", true).apply()

    // ── Daily sync ────────────────────────────────────────────────────

    fun getLastDailySyncDate(): LocalDate? {
        val saved = prefs.getString("last_daily_sync_date", null)
        return if (saved != null) LocalDate.parse(saved) else null
    }

    fun markDailySyncDone() {
        prefs.edit().putString("last_daily_sync_date", LocalDate.now().toString()).apply()
    }

    fun needsDailySync(): Boolean {
        val last = getLastDailySyncDate() ?: return true
        return last.isBefore(LocalDate.now())
    }
}