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
import com.layzbug.app.ui.theme.OnPrimary
import com.layzbug.app.ui.theme.OnSurface
import com.layzbug.app.ui.theme.OnSurfaceVariant
import com.layzbug.app.ui.theme.OutlineVariant
import com.layzbug.app.ui.theme.Primary
import com.layzbug.app.ui.theme.Tertiary
import com.layzbug.app.ui.theme.SurfaceContainer

@Composable
fun CalendarGrid(
    days: List<CalendarDayModel>,
    onDayClick: (CalendarDayModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceBase)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(bottom = Dimens.spaceBase),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Weekday Headers (Now inside the grid for perfect alignment)
            items(weekDays) { day ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.spaceBase),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1
                    )
                }
            }

            // 2. The Days Grid
            items(
                items = days,
                key = { it.date.toString() } // IMPORTANT: This prevents unnecessary redraws
            ) { dayModel ->
                Box(contentAlignment = Alignment.Center) {
                    CalendarDayItem(
                        dayNumber = dayModel.date.dayOfMonth,
                        isWalked = dayModel.walked,
                        onClick = { onDayClick(dayModel) }
                    )
                }
            }
        }
    }
}

/**
 * Visual UI for an individual day in the grid.
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
            .size(Dimens.size4xl) // Forces a perfect 40dp circle
            .clip(CircleShape)
            .background(
                if (isWalked) Primary
                else SurfaceContainer,
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = if (isWalked) {
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