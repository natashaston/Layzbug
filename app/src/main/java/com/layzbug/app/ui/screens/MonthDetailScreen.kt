package com.layzbug.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.layzbug.app.ui.components.CalendarGrid
import com.layzbug.app.ui.components.EditWalkStatusBottomSheet
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceColor
import com.layzbug.app.data.viewmodel.MonthViewModel
import com.layzbug.app.ui.components.MonthHeroPill
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDetailScreen(
    onBack: () -> Unit,
    year: Int = YearMonth.now().year,
    month: Int = YearMonth.now().monthValue,
    viewModel: MonthViewModel = hiltViewModel()
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    // Use the passed parameters instead of YearMonth.now()
    val currentMonth = remember(year, month) { YearMonth.of(year, month) }

    val walkDays by viewModel.walkDays.collectAsState()
    val rawMonthStats by viewModel.monthStats.collectAsState()

    val displayedStats = rawMonthStats

    LaunchedEffect(currentMonth) {
        viewModel.loadMonthData(currentMonth)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            MonthHeroPill(
                stats = displayedStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(238.dp)
            )

            Spacer(modifier = Modifier.height(Dimens.spaceLg))

            CalendarGrid(
                days = walkDays,
                onDayClick = { clickedDay ->
                    selectedDate = clickedDay.date
                    showEditSheet = true
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showEditSheet && selectedDate != null) {
        val walkDay = walkDays.find { it.date == selectedDate }
        EditWalkStatusBottomSheet(
            isVisible = showEditSheet,
            dateLabel = "${selectedDate?.month?.name?.lowercase()?.replaceFirstChar { it.uppercase() }} ${selectedDate?.dayOfMonth}",
            currentStatus = walkDay?.walked ?: false,
            onWalked = {
                selectedDate?.let { viewModel.setWalkStatus(it, true) }
                showEditSheet = false
            },
            onNotWalked = {
                selectedDate?.let { viewModel.setWalkStatus(it, false) }
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false }
        )
    }
}