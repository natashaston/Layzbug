package com.layzbug.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.repository.WalkRepository
import com.layzbug.app.domain.StatsValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class HomeWalkDay(val label: String, val isWalked: Boolean, val date: LocalDate)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fitSyncManager: FitSyncManager,
    private val walkRepository: WalkRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val startOfWeek = today.minusDays(6)

    // Observe the full year 2026 for the Yearly card
    val yearlyWalks: StateFlow<StatsValue> = walkRepository.getWalksInRange(
        LocalDate.of(2026, 1, 1),
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
            (0..6).forEach { i ->
                val date = startOfWeek.plusDays(i.toLong())
                // One-way sync: only sync if DB says false
                if (!walkRepository.getWalkStatus(date)) {
                    if (fitSyncManager.checkWalkingGoal(date)) {
                        walkRepository.updateWalk(date, true)
                    }
                }
            }
        }
    }

    fun toggleDay(date: LocalDate, currentStatus: Boolean) {
        viewModelScope.launch {
            walkRepository.updateWalk(date, !currentStatus)
        }
    }
}