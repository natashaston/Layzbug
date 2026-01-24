package com.layzbug.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.data.viewmodel.CalendarDayModel
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.GoogleSansFlex

@Composable
fun CalendarGrid(
    days: List<CalendarDayModel>,
    onDayClick: (CalendarDayModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 1. Weekday Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.spaceXs),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.W500,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. The Days Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(vertical = Dimens.spaceBase),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(days) { dayModel ->
                // Using the visual component defined below
                CalendarDayItem(
                    dayNumber = dayModel.date.dayOfMonth,
                    isWalked = dayModel.walked,
                    onClick = { onDayClick(dayModel) }
                )
            }
        }
    }
}

/**
 * Visual UI for an individual day in the grid.
 * Renamed to CalendarDayItem to avoid conflict with old files.
 */
@Composable
fun CalendarDayItem(
    dayNumber: Int,
    isWalked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f) // Keeps it perfectly circular
            .clip(CircleShape)
            .background(
                if (isWalked) Color(0xFF81C784)
                else Color.LightGray.copy(alpha = 0.3f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber.toString(),
            color = if (isWalked) Color.White else Color.Black,
            fontSize = 14.sp,
            fontWeight = if (isWalked) FontWeight.Bold else FontWeight.Normal
        )
    }
}