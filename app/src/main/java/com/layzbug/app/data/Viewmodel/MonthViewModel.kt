package com.layzbug.app.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.repository.WalkRepository
import com.layzbug.app.domain.StatsValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import com.layzbug.app.data.local.WalkEntity

data class CalendarDayModel(val date: LocalDate, val walked: Boolean)

@HiltViewModel
class MonthViewModel @Inject constructor(
    private val fitSyncManager: FitSyncManager,
    private val walkRepository: WalkRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val walkDays: StateFlow<List<CalendarDayModel>> = _currentMonth.flatMapLatest { month ->
        // Try to get cached data first
        val cached = walkRepository.getCachedMonthData(month.year, month.monthValue)

        if (cached != null) {
            // Return cached data immediately
            flowOf(buildCalendarDays(month, cached))
        } else {
            // Load from database
            walkRepository.getWalksForMonth(month.year, month.monthValue).map { entities ->
                buildCalendarDays(month, entities)
            }
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = buildInitialCalendarDays(YearMonth.now())
        )

    val monthStats: StateFlow<StatsValue> = walkDays.map { days ->
        val count = days.count { it.walked }
        val monthName = _currentMonth.value.month.name.lowercase().replaceFirstChar { it.uppercase() }
        StatsValue(
            value = count,
            label = "Walks in $monthName"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildInitialStats(YearMonth.now())
    )

    private fun buildCalendarDays(month: YearMonth, entities: List<WalkEntity>): List<CalendarDayModel> {
        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            CalendarDayModel(
                date = date,
                walked = entities.find { it.date == date }?.isWalked ?: false
            )
        }
    }

    private fun buildInitialCalendarDays(month: YearMonth): List<CalendarDayModel> {
        // Use cached data if available for instant initial value
        val cached = walkRepository.getCachedMonthData(month.year, month.monthValue)
        return if (cached != null) {
            buildCalendarDays(month, cached)
        } else {
            emptyList()
        }
    }

    private fun buildInitialStats(month: YearMonth): StatsValue {
        val cached = walkRepository.getCachedMonthData(month.year, month.monthValue)
        val count = cached?.count { it.isWalked } ?: 0
        val monthName = month.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return StatsValue(value = count, label = "Walks in $monthName")
    }

    fun loadMonthData(month: YearMonth) {
        if (_currentMonth.value != month) {
            _currentMonth.value = month
        }
    }

    fun setWalkStatus(date: LocalDate, status: Boolean) {
        viewModelScope.launch {
            walkRepository.updateWalk(date, status)
        }
    }
}