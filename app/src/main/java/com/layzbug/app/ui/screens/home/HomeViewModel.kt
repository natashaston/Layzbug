package com.layzbug.app.ui.screens.home

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.repository.WalkRepository
import com.layzbug.app.domain.StatsValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class HomeWalkDay(val label: String, val isWalked: Boolean, val date: LocalDate)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fitSyncManager: FitSyncManager,
    private val walkRepository: WalkRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val startOfYear = LocalDate.of(2026, 1, 1)
    private val startOfWeek = today.minusDays(6)

    private val _isSyncing = MutableStateFlow(true)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // --- NEW: Permission check for the Splash Screen ---
    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    suspend fun checkPermissions(): Boolean {
        return try {
            // This calls a helper in your FitSyncManager (ensure it exists)
            val granted = fitSyncManager.hasPermissions(requiredPermissions)
            Log.d("LayzbugSync", "Permissions granted: $granted")
            granted
        } catch (e: Exception) {
            false
        }
    }

    val yearlyWalks: StateFlow<StatsValue> = walkRepository.getWalksInRange(
        startOfYear,
        LocalDate.of(2026, 12, 31)
    ).map { walks ->
        StatsValue(value = walks.count { it.isWalked }, label = "Yearly")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsValue(0, "Yearly"))

    val januaryWalks: StateFlow<StatsValue> = walkRepository.getWalksForMonth(2026, 1)
        .map { walks ->
            StatsValue(value = walks.count { it.isWalked }, label = "January")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsValue(0, "January"))

    val weeklyDays: StateFlow<List<HomeWalkDay>> = walkRepository.getWalksInRange(startOfWeek, today)
        .map { walks ->
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
        autoSyncSteps()
    }

    private fun autoSyncSteps() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // First, ensure we have permissions before even trying to sync
                if (!checkPermissions()) {
                    Log.w("LayzbugSync", "Aborting sync: Permissions not granted")
                    return@launch
                }

                val daysToSync = ChronoUnit.DAYS.between(startOfYear, today)

                for (i in 0..daysToSync) {
                    val date = startOfYear.plusDays(i)
                    val isWalkedInDb = walkRepository.getWalkStatus(date)

                    if (!isWalkedInDb) {
                        val hasMetGoal = fitSyncManager.checkWalkingGoal(date)
                        if (hasMetGoal) {
                            walkRepository.updateWalk(date, true)
                            delay(50)
                            Log.d("LayzbugSync", "Successfully synced $date")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LayzbugSync", "Sync failed: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun toggleDay(date: LocalDate, currentStatus: Boolean) {
        viewModelScope.launch {
            walkRepository.updateWalk(date, !currentStatus)
        }
    }
}