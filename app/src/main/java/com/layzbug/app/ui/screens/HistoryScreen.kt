package com.layzbug.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.layzbug.app.ui.components.MonthCard
import com.layzbug.app.ui.components.YearlyStatsWithDropdown
import com.layzbug.app.ui.theme.Dimens

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onNavigateToMonth: (year: Int, month: Int) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val monthsStats by viewModel.monthsStats.collectAsState()
    val yearTotal by viewModel.yearTotal.collectAsState()

    val years = (2020..2030).toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Yearly Stats Card with Dropdown
        YearlyStatsWithDropdown(
            totalWalks = yearTotal,
            selectedYear = selectedYear,
            availableYears = years,
            onYearSelected = { year -> viewModel.setSelectedYear(year) },
            modifier = Modifier.padding(vertical = Dimens.spaceBase)
        )

        // Months Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            modifier = Modifier.fillMaxSize()
        ) {
            items(monthsStats) { monthStat ->
                MonthCard(
                    monthName = monthStat.monthName,
                    walkCount = monthStat.walkCount,
                    isEnabled = monthStat.isEnabled,
                    onClick = {
                        if (monthStat.isEnabled) {
                            onNavigateToMonth(monthStat.year, monthStat.month)
                        }
                    }
                )
            }
        }
    }
}