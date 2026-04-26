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
import com.layzbug.app.data.auth.AuthManager
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
    private val installationTracker: InstallationTracker,
    private val authManager: AuthManager
) : ViewModel() {

    private val today = LocalDate.now()
    private val startOfYear = installationTracker.getSyncStartDate()
    private val currentYearStart = LocalDate.of(today.year, 1, 1)
    private val currentYearEnd = LocalDate.of(today.year, 12, 31)
    private val startOfWeek = today.minusDays(6)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncCompleted = MutableStateFlow(false)
    val syncCompleted: StateFlow<Boolean> = _syncCompleted.asStateFlow()

    private val _syncToastDismissed = MutableStateFlow(false)
    val syncToastDismissed: StateFlow<Boolean> = _syncToastDismissed.asStateFlow()

    // ── Auth state as a StateFlow so any screen can drive it ──────────
    private val _isLoggedIn = MutableStateFlow(authManager.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Persisted in SharedPreferences — survives process death
    private var hasInitialSyncCompleted = installationTracker.hasInitialSyncDone()
    private val _refreshTrigger = MutableStateFlow(0)

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    init {
        Log.d("LayzbugSync", "📅 Sync start date: $startOfYear")

        viewModelScope.launch {
            delay(500)
            if (walkRepository.isSupabaseLoggedIn()) {
                Log.d("LayzbugSync", "✅ Already logged in, starting Supabase listener")
                walkRepository.startSupabaseSync()
            }
            if (!hasInitialSyncCompleted) {
                val hasPerms = checkPermissions()
                if (hasPerms) startInitialSync()
            }
        }
    }

    suspend fun checkPermissions(): Boolean {
        return try {
            fitSyncManager.hasPermissions(requiredPermissions)
        } catch (e: Exception) {
            Log.e("LayzbugSync", "Permission check failed: ${e.message}")
            false
        }
    }

    val yearlyWalks: StateFlow<StatsValue> = combine(
        walkRepository.getWalksInRange(currentYearStart, currentYearEnd),
        _refreshTrigger
    ) { walks, _ ->
        val walkCount    = walks.count { it.isWalked }
        val totalDistance = Math.round(walks.sumOf { it.distanceKm } * 10.0) / 10.0
        val totalMinutes  = walks.sumOf { it.minutes }
        StatsValue(value = walkCount, label = "Yearly", distanceKm = totalDistance, totalMinutes = totalMinutes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsValue(0, "Yearly"))

    val currentMonthWalks: StateFlow<StatsValue> = combine(
        walkRepository.getWalksForMonth(today.year, today.monthValue),
        _refreshTrigger
    ) { walks, _ ->
        val monthName     = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val walkCount     = walks.count { it.isWalked }
        val totalDistance = Math.round(walks.sumOf { it.distanceKm } * 10.0) / 10.0
        val totalMinutes  = walks.sumOf { it.minutes }
        StatsValue(value = walkCount, label = monthName, distanceKm = totalDistance, totalMinutes = totalMinutes)
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
            val date   = startOfWeek.plusDays(i.toLong())
            val record = walks.find { it.date == date }
            HomeWalkDay(
                label    = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                isWalked = record?.isWalked ?: false,
                date     = date
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startInitialSync() {
        if (hasInitialSyncCompleted) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                withContext(Dispatchers.IO) {
                    if (walkRepository.isSupabaseLoggedIn()) {
                        walkRepository.syncFromSupabase()
                        walkRepository.startSupabaseSync()
                    }
                    withTimeout(120_000) { autoSyncSteps() }
                }
                delay(1000)
                _refreshTrigger.value++
                hasInitialSyncCompleted = true
                installationTracker.markInitialSyncDone()  // persist so it never runs again
                _syncCompleted.value = true
            } catch (e: Exception) {
                Log.e("LayzbugSync", "Sync failed: ${e.message}", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun autoSyncSteps() {
        if (!checkPermissions()) return
        val daysToSync = ChronoUnit.DAYS.between(startOfYear, today)
        for (i in daysToSync downTo 0) {
            val date = startOfYear.plusDays(i)
            try {
                val result = fitSyncManager.checkDailyWalk(date)
                walkRepository.updateWalkFromGoogleFit(date, result.isWalked, result.distanceKm, result.totalMinutes)
                delay(50)
            } catch (e: Exception) {
                Log.e("LayzbugSync", "Error syncing $date: ${e.message}")
            }
        }
    }

    fun toggleDay(date: LocalDate, currentStatus: Boolean) {
        viewModelScope.launch {
            walkRepository.updateManualWalk(date, !currentStatus)
        }
    }

    /**
     * Called after sign-in from ANY screen.
     * Updates the shared isLoggedIn state immediately.
     */
    fun onUserSignedIn() {
        viewModelScope.launch {
            _isLoggedIn.value = true
            try {
                walkRepository.syncPendingManualWalks()
                walkRepository.syncFromSupabase()
                walkRepository.startSupabaseSync()
                _refreshTrigger.value++
            } catch (e: Exception) {
                Log.e("LayzbugSync", "Post-login sync failed: ${e.message}", e)
            }
        }
    }

    fun onUserSignedOut() {
        _isLoggedIn.value = false
    }

    fun dismissSyncToast() {
        _syncToastDismissed.value = true
    }
}