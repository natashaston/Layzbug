package com.layzbug.app.ui.screens.home

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.InstallationTracker
import com.layzbug.app.data.auth.AuthManager
import com.layzbug.app.data.repository.WalkRepository
import com.layzbug.app.domain.StatsValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Instant
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
    private val authManager: AuthManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val today            = LocalDate.now()
    private val startOfYear      = installationTracker.getSyncStartDate()
    private val currentYearStart = LocalDate.of(today.year, 1, 1)
    private val currentYearEnd   = LocalDate.of(today.year, 12, 31)
    private val startOfWeek      = today.minusDays(6)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncCompleted = MutableStateFlow(false)
    val syncCompleted: StateFlow<Boolean> = _syncCompleted.asStateFlow()

    private val _syncToastDismissed = MutableStateFlow(false)
    val syncToastDismissed: StateFlow<Boolean> = _syncToastDismissed.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(authManager.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _fitnessConnected = MutableStateFlow<Boolean?>(null)
    val fitnessConnected: StateFlow<Boolean?> = _fitnessConnected.asStateFlow()

    private val _hcPermissionsGranted = MutableStateFlow(false)
    val hcPermissionsGranted: StateFlow<Boolean> = _hcPermissionsGranted.asStateFlow()

    // 0f = not started, 1f = complete. Updated per-day during autoSyncSteps.
    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private var hasInitialSyncCompleted = installationTracker.hasInitialSyncDone()
    private val syncMutex = Mutex()
    private val _refreshTrigger = MutableStateFlow(0)

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
    )

    private val detectionPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    private suspend fun checkHcPermissionsDirect(): Boolean {
        return try {
            val client  = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            val hasAll  = detectionPermissions.all { it in granted }
            Log.d("SyncDebug", "checkHcPermissionsDirect: hasAll=$hasAll grantedCount=${granted.size}")
            hasAll
        } catch (e: Exception) {
            Log.e("SyncDebug", "checkHcPermissionsDirect failed: ${e.message}")
            false
        }
    }

    init {
        Log.d("SyncDebug", "📅 Sync start date: $startOfYear")

        viewModelScope.launch {
            delay(500)

            if (walkRepository.isSupabaseLoggedIn()) {
                walkRepository.startSupabaseSync()
            }

            val hasPerms = checkHcPermissionsDirect()
            Log.d("SyncDebug", "init: hasPerms=$hasPerms hasInitialSyncCompleted=$hasInitialSyncCompleted")
            _hcPermissionsGranted.value = hasPerms

            if (!hasInitialSyncCompleted && hasPerms) {
                Log.d("SyncDebug", "init: starting initial sync")
                startInitialSync()
            }

            checkFitnessConnection()
        }

        viewModelScope.launch {
            _hcPermissionsGranted
                .filter { it }
                .collect {
                    Log.d("SyncDebug", "collector fired: hasInitialSyncCompleted=$hasInitialSyncCompleted")
                    if (!hasInitialSyncCompleted) {
                        Log.d("SyncDebug", "collector: calling startInitialSync()")
                        startInitialSync()
                    }
                }
        }
    }

    fun hasInitialSyncCompleted(): Boolean = hasInitialSyncCompleted

    fun onPermissionsGranted() {
        Log.d("SyncDebug", "onPermissionsGranted called")
        _hcPermissionsGranted.value = true
        if (!hasInitialSyncCompleted) {
            startInitialSync()
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

    fun checkFitnessConnection() {
        viewModelScope.launch {
            val connected = withContext(Dispatchers.IO) {
                try {
                    val client   = HealthConnectClient.getOrCreate(context)
                    val end      = Instant.now()
                    val start    = end.minus(30, ChronoUnit.DAYS)
                    val response = client.readRecords(
                        ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end))
                    )
                    response.records.isNotEmpty()
                } catch (e: Exception) {
                    Log.e("LayzbugSync", "Fitness connection check failed: ${e.message}")
                    false
                }
            }
            Log.d("SyncDebug", "checkFitnessConnection: connected=$connected")
            _fitnessConnected.value = connected
        }
    }

    val yearlyWalks: StateFlow<StatsValue> = combine(
        walkRepository.getWalksInRange(currentYearStart, currentYearEnd),
        _refreshTrigger
    ) { walks, _ ->
        val walkCount     = walks.count { it.isWalked }
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
        if (hasInitialSyncCompleted) {
            Log.d("SyncDebug", "startInitialSync: already done, skipping")
            return
        }

        viewModelScope.launch {
            val acquired = syncMutex.tryLock()
            if (!acquired) {
                Log.d("SyncDebug", "startInitialSync: mutex locked — already running")
                return@launch
            }
            try {
                if (hasInitialSyncCompleted) {
                    Log.d("SyncDebug", "startInitialSync: already done after lock")
                    return@launch
                }
                _syncProgress.value = 0f
                _isSyncing.value = true
                Log.d("SyncDebug", "startInitialSync: sync started")
                checkFitnessConnection()

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
                installationTracker.markInitialSyncDone()
                _syncProgress.value = 1f
                _syncCompleted.value = true
                Log.d("SyncDebug", "startInitialSync: sync finished successfully")
                checkFitnessConnection()
            } catch (e: Exception) {
                Log.e("LayzbugSync", "Sync failed: ${e.message}", e)
            } finally {
                _isSyncing.value = false
                syncMutex.unlock()
            }
        }
    }

    private suspend fun autoSyncSteps() {
        val hasPerms = checkHcPermissionsDirect()
        if (!hasPerms) {
            Log.d("SyncDebug", "autoSyncSteps: no data permissions, skipping")
            return
        }
        val daysToSync = ChronoUnit.DAYS.between(startOfYear, today)
        Log.d("SyncDebug", "autoSyncSteps: syncing $daysToSync days")
        for (i in daysToSync downTo 0) {
            val date = startOfYear.plusDays(i)
            try {
                val result = fitSyncManager.checkDailyWalk(date)
                walkRepository.updateWalkFromGoogleFit(date, result.isWalked, result.distanceKm, result.totalMinutes)
                delay(50)
            } catch (e: Exception) {
                Log.e("LayzbugSync", "Error syncing $date: ${e.message}")
            }
            // Update progress after each day — i goes high→low so completed = daysToSync - i
            val completed = daysToSync - i
            _syncProgress.value = if (daysToSync > 0) completed / daysToSync.toFloat() else 1f
        }
    }

    fun syncTodayIfNeeded() {
        viewModelScope.launch {
            val hasPerms = checkHcPermissionsDirect()
            Log.d("SyncDebug", "syncTodayIfNeeded: hasPerms=$hasPerms _hcPermissionsGranted=${_hcPermissionsGranted.value} hasInitialSyncCompleted=$hasInitialSyncCompleted")

            if (hasPerms && !_hcPermissionsGranted.value) {
                Log.d("SyncDebug", "syncTodayIfNeeded: permissions newly detected")
                _hcPermissionsGranted.value = true
            } else if (hasPerms && !hasInitialSyncCompleted) {
                Log.d("SyncDebug", "syncTodayIfNeeded: perms known but sync not done — calling startInitialSync")
                startInitialSync()
            }

            checkFitnessConnection()
        }

        if (!hasInitialSyncCompleted) return

        viewModelScope.launch {
            val hasPerms = checkPermissions()
            if (!hasPerms) return@launch

            try {
                withContext(Dispatchers.IO) {
                    val lastSync   = installationTracker.getLastDailySyncDate() ?: today
                    val daysToSync = ChronoUnit.DAYS.between(lastSync, today)

                    for (i in 0..daysToSync) {
                        val date   = lastSync.plusDays(i)
                        val result = fitSyncManager.checkDailyWalk(date)
                        walkRepository.updateWalkFromGoogleFit(
                            date, result.isWalked, result.distanceKm, result.totalMinutes
                        )
                        Log.d("LayzbugSync", "✅ Silent sync: $date ${result.totalMinutes}min walked=${result.isWalked}")
                    }

                    if (daysToSync > 0) {
                        installationTracker.markDailySyncDone()
                    }
                }
                _refreshTrigger.value++
            } catch (e: Exception) {
                Log.e("LayzbugSync", "Silent daily sync failed: ${e.message}")
            }
        }
    }

    fun toggleDay(date: LocalDate, currentStatus: Boolean) {
        viewModelScope.launch {
            walkRepository.updateManualWalk(date, !currentStatus)
        }
    }

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