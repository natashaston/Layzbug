package com.layzbug.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.layzbug.app.domain.StatsValue
import com.layzbug.app.ui.components.CalendarGrid
import com.layzbug.app.ui.components.EditWalkStatusBottomSheet
import com.layzbug.app.ui.components.StatsCardPill
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.Surface
import com.layzbug.app.data.viewmodel.MonthViewModel
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDetailScreen(
    onBack: () -> Unit,
    viewModel: MonthViewModel = hiltViewModel()
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    val currentMonth = remember { YearMonth.of(2026, 1) }
    val walkDays by viewModel.walkDays.collectAsState()

    LaunchedEffect(currentMonth) {
        viewModel.loadMonthData(currentMonth)
    }

    val monthStats = remember(walkDays) {
        val count = walkDays.count { it.walked }
        StatsValue(
            value = count,
            label = "Walks in ${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }}"
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Surface).padding(horizontal = 16.dp)) {
        StatsCardPill(
            stats = monthStats,
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Dimens.spaceLg))

        CalendarGrid(
            days = walkDays,
            onDayClick = { clickedDay ->
                selectedDate = clickedDay.date
                showEditSheet = true
            },
            modifier = Modifier.fillMaxSize()
        )
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