package com.layzbug.app.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.repository.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

// Keep this unified model
data class CalendarDayModel(val date: LocalDate, val walked: Boolean)

@HiltViewModel
class MonthViewModel @Inject constructor(
    private val fitSyncManager: FitSyncManager,
    private val walkRepository: WalkRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.of(2026, 1))

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val walkDays: StateFlow<List<CalendarDayModel>> = _currentMonth.flatMapLatest { month ->
        walkRepository.getWalksForMonth(month.year, month.monthValue).map { entities ->
            (1..month.lengthOfMonth()).map { day ->
                val date = month.atDay(day)
                CalendarDayModel(
                    date = date,
                    // Safe lookup to prevent crashes if entities is empty
                    walked = entities?.find { it.date == date }?.isWalked ?: false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadMonthData(month: YearMonth) {
        _currentMonth.value = month
        syncMonthWithFit(month)
    }

    private fun syncMonthWithFit(month: YearMonth) {
        viewModelScope.launch {
            (1..month.lengthOfMonth()).forEach { day ->
                val date = month.atDay(day)
                // One-way sync logic: only update if not already walked
                val isAlreadyWalked = walkRepository.getWalkStatus(date)
                if (!isAlreadyWalked) {
                    val metGoal = fitSyncManager.checkWalkingGoal(date)
                    if (metGoal) {
                        walkRepository.updateWalk(date, true)
                    }
                }
            }
        }
    }

    fun setWalkStatus(date: LocalDate, status: Boolean) {
        viewModelScope.launch {
            walkRepository.updateWalk(date, status)
        }
    }
}