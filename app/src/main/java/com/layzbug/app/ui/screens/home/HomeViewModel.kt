package com.layzbug.app.ui.screens.home

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.InstallationTracker
import com.layzbug.app.data.repository.WalkRepository
import com.layzbug.app.domain.StatsValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class HomeWalkDay(val label: String, val isWalked: Boolean, val date: LocalDate)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fitSyncManager: FitSyncManager,
    private val walkRepository: WalkRepository,
    private val installationTracker: InstallationTracker
) : ViewModel() {

    private val today = LocalDate.now()

    // Dynamic: Start from beginning of installation year
    private val startOfYear = installationTracker.getSyncStartDate()

    // Dynamic: Current year start and end
    private val currentYearStart = LocalDate.of(today.year, 1, 1)
    private val currentYearEnd = LocalDate.of(today.year, 12, 31)

    private val startOfWeek = today.minusDays(6)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Debug: visible sync status for troubleshooting
    private var hasInitialSyncCompleted = false

    // Refresh trigger to force UI updates after Google Fit sync
    private val _refreshTrigger = MutableStateFlow(0)

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
    )

    init {
        Log.d("LayzbugSync", "📅 Installation date: ${installationTracker.getInstallationDate()}")
        Log.d("LayzbugSync", "📅 Sync start date: $startOfYear")
        Log.d("LayzbugSync", "📅 Current year: ${today.year}")
    }

    suspend fun checkPermissions(): Boolean {
        return try {
            val granted = fitSyncManager.hasPermissions(requiredPermissions)
            Log.d("LayzbugSync", "Permissions granted: $granted")
            granted
        } catch (e: Exception) {
            Log.e("LayzbugSync", "Permission check failed: ${e.message}")
            false
        }
    }

    // Shows CURRENT YEAR walks + distance
    val yearlyWalks: StateFlow<StatsValue> = combine(
        walkRepository.getWalksInRange(currentYearStart, currentYearEnd),
        _refreshTrigger
    ) { walks, _ ->
        val walkCount = walks.count { it.isWalked }
        val totalDistance = Math.round(walks.sumOf { it.distanceKm } * 10.0) / 10.0
        StatsValue(value = walkCount, label = "Yearly", distanceKm = totalDistance)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsValue(0, "Yearly"))

    val currentMonthWalks: StateFlow<StatsValue> = combine(
        walkRepository.getWalksForMonth(today.year, today.monthValue),
        _refreshTrigger
    ) { walks, _ ->
        val monthName = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val walkCount = walks.count { it.isWalked }
        val totalDistance = Math.round(walks.sumOf { it.distanceKm } * 10.0) / 10.0
        StatsValue(value = walkCount, label = monthName, distanceKm = totalDistance)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StatsValue(0, today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()))
    )

    val weeklyDays: StateFlow<List<HomeWalkDay>> = combine(
        walkRepository.getWalksInRange(startOfWeek, today),
        _refreshTrigger
    ) { walks, _ ->
        (0..6).map { i ->
            val date = startOfWeek.plusDays(i.toLong())
            val record = walks.find { it.date == date }
            HomeWalkDay(
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                isWalked = record?.isWalked ?: false,
                date = date
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        Log.d("LayzbugSync", "HomeViewModel initialized - NOT auto-syncing")

        // Check if we should auto-sync (coming from permission grant)
        viewModelScope.launch {
            delay(500)

            if (walkRepository.isSupabaseLoggedIn()) {
                Log.d("LayzbugSync", "✅ Already logged in, starting Supabase listener")
                walkRepository.startSupabaseSync()
            } else {
                Log.d("LayzbugSync", "⚠️ Not logged in, skipping Supabase listener")
            }

            // Auto-trigger initial sync if permissions just granted
            if (!hasInitialSyncCompleted) {
                val hasPerms = checkPermissions()
                if (hasPerms) {
                    Log.d("LayzbugSync", "🎯 Permissions detected, auto-starting sync")
                    startInitialSync()
                }
            }
        }
    }

    fun startInitialSync() {
        if (hasInitialSyncCompleted) {
            Log.d("LayzbugSync", "⏭️ Skipping sync - already completed")
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                Log.d("LayzbugSync", "🚀 Starting initial sync...")

                withContext(Dispatchers.IO) {
                    // Check if Supabase is active
                    if (walkRepository.isSupabaseLoggedIn()) {
                        Log.d("LayzbugSync", "✅ Supabase is active, syncing manual walks...")
                        walkRepository.syncFromSupabase()
                        walkRepository.startSupabaseSync()
                        Log.d("LayzbugSync", "✅ Now syncing from Google Fit...")
                        withTimeout(120_000) {
                            autoSyncSteps()
                        }
                    } else {
                        Log.d("LayzbugSync", "⚠️ Not logged in, syncing from Google Fit...")
                        withTimeout(120_000) {
                            autoSyncSteps()
                        }
                    }
                }

                // Force UI refresh after sync completes
                Log.d("LayzbugSync", "⏳ Waiting for database writes to complete...")
                delay(1000)  // Give time for all DB writes to commit

                _refreshTrigger.value++
                Log.d("LayzbugSync", "🔄 Triggered UI refresh")

                hasInitialSyncCompleted = true
                Log.d("LayzbugSync", "✅ Initial sync complete")
            } catch (e: Exception) {
                Log.e("LayzbugSync", "❌ Sync failed: ${e.message}", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun autoSyncSteps() {
        if (!checkPermissions()) {
            Log.w("LayzbugSync", "⚠️ Aborting sync: Permissions not granted")
            return
        }

        // Sync from installation year start to today
        val daysToSync = ChronoUnit.DAYS.between(startOfYear, today)
        Log.d("LayzbugSync", "Syncing $daysToSync days from Google Fit (from $startOfYear to $today)...")

        var syncedCount = 0

        for (i in daysToSync downTo 0) {
            val date = startOfYear.plusDays(i)

            try {
                val isWalkedInDb = walkRepository.getWalkStatus(date)

                if (!isWalkedInDb) {
                    // Day not yet marked as walked — check Health Connect
                    val result = fitSyncManager.checkDailyWalk(date)
                    if (result.isWalked) {
                        walkRepository.updateWalkFromGoogleFit(date, true, result.distanceKm)
                        syncedCount++
                        delay(50)
                    } else if (result.distanceKm > 0) {
                        // Not walked but has distance data — store it
                        walkRepository.updateWalkFromGoogleFit(date, false, result.distanceKm)
                        delay(50)
                    }
                }
            } catch (e: Exception) {
                Log.e("LayzbugSync", "Error syncing $date: ${e.message}")
            }
        }

        Log.d("LayzbugSync", "✅ Synced $syncedCount new walks from Google Fit")
    }

    fun toggleDay(date: LocalDate, currentStatus: Boolean) {
        viewModelScope.launch {
            walkRepository.updateManualWalk(date, !currentStatus)
        }
    }

    /**
     * Called after user signs in (e.g. from HomeScreen banner).
     * Re-syncs manual walks from Supabase and starts listener.
     */
    fun onUserSignedIn() {
        viewModelScope.launch {
            try {
                Log.d("LayzbugSync", "🔄 User signed in — syncing from Supabase...")
                walkRepository.syncFromSupabase()
                walkRepository.startSupabaseSync()
                _refreshTrigger.value++
                Log.d("LayzbugSync", "✅ Post-login Supabase sync complete")
            } catch (e: Exception) {
                Log.e("LayzbugSync", "❌ Post-login sync failed: ${e.message}", e)
            }
        }
    }
}