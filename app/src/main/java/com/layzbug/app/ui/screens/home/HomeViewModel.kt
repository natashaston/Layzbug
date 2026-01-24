package com.layzbug.app.ui.screens.home

import android.util.Log
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

    // --- CLASS PROPERTIES (Scope fix) ---
    private val today = LocalDate.now()
    private val startOfYear = LocalDate.of(2026, 1, 1)
    private val startOfWeek = today.minusDays(6)

    // Observe the full year 2026 for the Yearly card
    val yearlyWalks: StateFlow<StatsValue> = walkRepository.getWalksInRange(
        startOfYear,
        LocalDate.of(2026, 12, 31)
    ).map { walks ->
        StatsValue(value = walks.count { it.isWalked }, label = "Yearly")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsValue(0, "Yearly"))

    // Observe January 2026 for the January card
    val januaryWalks: StateFlow<StatsValue> = walkRepository.getWalksForMonth(2026, 1)
        .map { walks ->
            StatsValue(value = walks.count { it.isWalked }, label = "January")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsValue(0, "January"))

    // Observe the last 7 days
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
            try {
                // Now startOfYear is visible here because it's a class property
                val daysToSync = ChronoUnit.DAYS.between(startOfYear, today)

                for (i in 0..daysToSync) {
                    val date = startOfYear.plusDays(i)

                    // One-way sync rule: Check local DB status first
                    val isWalkedInDb = walkRepository.getWalkStatus(date)

                    if (!isWalkedInDb) {
                        val hasMetGoal = fitSyncManager.checkWalkingGoal(date)
                        if (hasMetGoal) {
                            walkRepository.updateWalk(date, true)
                            // Small delay to ensure database flows update smoothly
                            delay(50)
                            Log.d("LayzbugSync", "Successfully synced $date")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LayzbugSync", "Sync failed: ${e.message}")
            }
        }
    }

    fun toggleDay(date: LocalDate, currentStatus: Boolean) {
        viewModelScope.launch {
            walkRepository.updateWalk(date, !currentStatus)
        }
    }
}