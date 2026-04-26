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
    // First install: save today as the anchor. Subsequent launches: return saved date.

    fun getSyncStartDate(): LocalDate {
        val saved = prefs.getString("sync_start_date", null)
        return if (saved != null) {
            LocalDate.parse(saved)
        } else {
            val oneYearAgo = LocalDate.now().minusYears(1)
            prefs.edit().putString("sync_start_date", oneYearAgo.toString()).apply()
            oneYearAgo
        }
    }

    // ── Initial full sync flag ────────────────────────────────────────
    // Once the long historical sync completes, this is persisted so it
    // never runs again on subsequent app launches.

    fun hasInitialSyncDone(): Boolean =
        prefs.getBoolean("initial_sync_done", false)

    fun markInitialSyncDone() =
        prefs.edit().putBoolean("initial_sync_done", true).apply()
}