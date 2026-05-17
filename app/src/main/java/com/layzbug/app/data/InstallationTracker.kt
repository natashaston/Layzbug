package com.layzbug.app.data

import android.content.Context
import java.time.LocalDate

class InstallationTracker(context: Context) {

    private val prefs = context.getSharedPreferences("layzbug_install", Context.MODE_PRIVATE)

    init {
        // ── Fresh install detection ───────────────────────────────────
        // Android Auto Backup restores SharedPreferences across reinstalls.
        // This causes onboarding_complete=true on a brand new install.
        //
        // Detection strategy:
        // - fresh_install_marker is written alongside onboarding_complete
        //   when onboarding finishes for the first time
        // - If onboarding_complete=true but fresh_install_marker is absent,
        //   it means onboarding_complete was restored by Auto Backup without
        //   a legitimate prior completion on this device → reset ONLY
        //   onboarding_complete so the user sees onboarding again
        // - We do NOT reset initial_sync_done or sync dates — those are
        //   valid data that should survive reinstalls if the user's HC
        //   permissions are still intact

        val onboardingComplete = prefs.getBoolean("onboarding_complete", false)
        val freshInstallMarker = prefs.getBoolean("fresh_install_marker", false)

        if (onboardingComplete && !freshInstallMarker) {
            // Restored by Auto Backup on a new install — reset only onboarding flag.
            // Keep sync dates and initial_sync_done intact.
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