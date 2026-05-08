package com.layzbug.app.data

import android.content.Context
import java.time.LocalDate

class InstallationTracker(context: Context) {

    private val prefs = context.getSharedPreferences("layzbug_install", Context.MODE_PRIVATE)

    // ── Onboarding ───────────────────────────────────────────────────

    fun isOnboardingComplete(): Boolean =
        prefs.getBoolean("onboarding_complete", false)

    fun setOnboardingComplete() =
        prefs.edit().putBoolean("onboarding_complete", true).apply()

    // ── Sync start date ───────────────────────────────────────────────
    // Logic:
    // - First install ever → save Jan 1 of current year as the anchor
    // - Returning user who previously installed → return their saved date
    //   (preserves original install year so historical data is fetched correctly)
    // - This means if someone installed in March 2026, sync starts Jan 1 2026
    // - If they reinstall after years away and log back in with same account,
    //   the saved date from Supabase/backend should be used — this covers
    //   the local device anchor only

    fun getSyncStartDate(): LocalDate {
        val saved = prefs.getString("sync_start_date", null)
        return if (saved != null) {
            LocalDate.parse(saved)
        } else {
            // New install — start from Jan 1 of current year
            val startOfYear = LocalDate.of(LocalDate.now().year, 1, 1)
            prefs.edit().putString("sync_start_date", startOfYear.toString()).apply()
            startOfYear
        }
    }

    // ── Initial full sync flag ────────────────────────────────────────
    // Once the long historical sync completes, persisted so it
    // never runs again on subsequent app launches.

    fun hasInitialSyncDone(): Boolean =
        prefs.getBoolean("initial_sync_done", false)

    fun markInitialSyncDone() =
        prefs.edit().putBoolean("initial_sync_done", true).apply()

    // ── Daily sync ────────────────────────────────────────────────────
    // Tracks the last date walks were re-synced from Health Connect.
    // Separate from initial sync — this runs once per calendar day, silently.
    // If the app hasn't been opened for multiple days, syncTodayIfNeeded()
    // uses this date as the anchor to back-fill all missed days.

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