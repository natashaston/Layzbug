package com.layzbug.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.layzbug.app.ui.components.StatsCard
import com.layzbug.app.ui.components.StatsCardPill
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.layzbug.app.domain.StatsValue
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.OnPrimary
import com.layzbug.app.ui.theme.OnSurface
import com.layzbug.app.ui.theme.OutlineVariant
import com.layzbug.app.ui.theme.Primary
import com.layzbug.app.ui.theme.SurfaceContainer
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.layzbug.app.data.viewmodel.MonthViewModel

@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToMonthDetail: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val monthViewModel: MonthViewModel = hiltViewModel()

    val yearlyWalks by viewModel.yearlyWalks.collectAsState(initial = StatsValue(0, "Yearly"))
    val currentMonthWalks by viewModel.currentMonthWalks.collectAsState(
        initial = StatsValue(0, LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault()))
    )
    val weeklyDays by viewModel.weeklyDays.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsCard(
                number = yearlyWalks.value.toString(),
                label = yearlyWalks.label,
                modifier = Modifier.weight(1f).graphicsLayer(),
                onClick = onNavigateToHistory
            )

            // Fixed: Removed the sharedTransitionScope logic that was causing errors
            StatsCardPill(
                stats = currentMonthWalks,
                modifier = Modifier.weight(1f).graphicsLayer(),
                onClick = onNavigateToMonthDetail
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spaceXl3))
        Text("Weekly Progress", color = OnSurface, style = MaterialTheme.typography.headlineMedium,)
        Spacer(modifier = Modifier.height(Dimens.spaceXs2))
        Text("30 minute walks", color = OnSurface, style = MaterialTheme.typography.bodySmall,)
        Spacer(modifier = Modifier.height(Dimens.spaceBase))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(300.dp)
        ) {
            items(weeklyDays) { day ->
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (day.isWalked) Primary else SurfaceContainer)
                        .clickable {
                            viewModel.toggleDay(day.date, day.isWalked)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.label,
                        style = if (day.isWalked) {
                            MaterialTheme.typography.bodySmall.copy(
                                color = OnPrimary
                            )
                        } else {
                            MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }
        }
    }
}
