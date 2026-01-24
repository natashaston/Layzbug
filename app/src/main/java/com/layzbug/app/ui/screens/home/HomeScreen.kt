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
import com.layzbug.app.domain.StatsValue

@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToMonthDetail: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()


    val yearlyWalks by viewModel.yearlyWalks.collectAsState(initial = StatsValue(0, "Yearly"))
    val januaryWalks by viewModel.januaryWalks.collectAsState(initial = StatsValue(0, "January"))
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
                modifier = Modifier.weight(1f),
                onClick = onNavigateToHistory
            )

            // Fixed: Removed the sharedTransitionScope logic that was causing errors
            StatsCardPill(
                stats = januaryWalks,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMonthDetail
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text("Weekly Progress", color = Color.Black, fontWeight = FontWeight.Bold)
        Text("Syncs 30m+ walks from Google Fit", color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

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
                        .background(if (day.isWalked) Color(0xFF81C784) else Color.LightGray.copy(alpha = 0.5f))
                        .clickable {
                            viewModel.toggleDay(day.date, day.isWalked)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.label,
                        color = if (day.isWalked) Color.White else Color.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}