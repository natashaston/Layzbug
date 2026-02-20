package com.layzbug.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layzbug.app.data.repository.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.launch

data class MonthStats(
    val year: Int,
    val month: Int,
    val monthName: String,
    val walkCount: Int,
    val isEnabled: Boolean
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val walkRepository: WalkRepository
) : ViewModel() {

    private val currentMonth = LocalDate.now().monthValue
    val currentYear = LocalDate.now().year

    // Dynamic years from database
    val availableYears: StateFlow<List<Int>> = walkRepository.getAvailableYears()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = listOf(currentYear)
        )

    private val _selectedYear = MutableStateFlow(currentYear)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    val monthsStats: StateFlow<List<MonthStats>> = _selectedYear.flatMapLatest { year ->
        combine(
            (1..12).map { month ->
                walkRepository.getWalksForMonth(year, month).map { walks ->
                    val monthName = YearMonth.of(year, month).month
                        .name.lowercase().replaceFirstChar { it.uppercase() }
                        .take(3)

                    MonthStats(
                        year = year,
                        month = month,
                        monthName = monthName,
                        walkCount = walks.count { it.isWalked },
                        isEnabled = if (year == currentYear) month <= currentMonth else year < currentYear
                    )
                }
            }
        ) { statsArray -> statsArray.toList() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildInitialMonthStats(currentYear)
    )

    val yearTotal: StateFlow<Int> = monthsStats.map { months ->
        months.sumOf { it.walkCount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = run {
            (1..12).sumOf { month ->
                walkRepository.getCachedMonthData(currentYear, month)?.count { it.isWalked } ?: 0
            }
        }
    )

    init {
        viewModelScope.launch {
            // When available years load, select the most recent one
            availableYears.collect { years ->
                if (years.isNotEmpty() && _selectedYear.value == currentYear) {
                    _selectedYear.value = years.first() // First = most recent
                }
            }
        }

        // Preload current year data
        viewModelScope.launch {
            (1..12).forEach { month ->
                walkRepository.getWalksForMonth(currentYear, month)
                    .take(1)
                    .collect { }
            }
        }
    }

    private fun buildInitialMonthStats(year: Int): List<MonthStats> {
        return (1..12).map { month ->
            val cached = walkRepository.getCachedMonthData(year, month)
            val monthName = YearMonth.of(year, month).month
                .name.lowercase().replaceFirstChar { it.uppercase() }
                .take(3)

            MonthStats(
                year = year,
                month = month,
                monthName = monthName,
                walkCount = cached?.count { it.isWalked } ?: 0,
                isEnabled = if (year == currentYear) month <= currentMonth else year < currentYear
            )
        }
    }

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
    }
}