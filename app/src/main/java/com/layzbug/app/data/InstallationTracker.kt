package com.layzbug.app.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the app's first installation date.
 * This date is used as the starting point for Google Fit sync.
 */
@Singleton
class InstallationTracker @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("layzbug_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_INSTALL_DATE = "install_date"
    }

    /**
     * Gets the installation date. If not set, sets it to today and returns it.
     * This ensures the date is captured on first launch.
     */
    fun getInstallationDate(): LocalDate {
        val savedDate = prefs.getString(KEY_INSTALL_DATE, null)

        return if (savedDate != null) {
            LocalDate.parse(savedDate)
        } else {
            // First time - save today as installation date
            val today = LocalDate.now()
            prefs.edit().putString(KEY_INSTALL_DATE, today.toString()).apply()
            today
        }
    }

    /**
     * Gets the sync start date - the beginning of the year when app was installed.
     * For example:
     * - Installed Nov 15, 2026 → Returns Jan 1, 2026
     * - Installed Feb 3, 2027 → Returns Jan 1, 2027
     */
    fun getSyncStartDate(): LocalDate {
        val installDate = getInstallationDate()
        return LocalDate.of(installDate.year, 1, 1) // Start of installation year
    }

    /**
     * For debugging - reset installation date
     */
    fun resetInstallationDate() {
        prefs.edit().remove(KEY_INSTALL_DATE).apply()
    }
}