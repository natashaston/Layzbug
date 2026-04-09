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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.R
import com.layzbug.app.data.viewmodel.CalendarDayModel
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceContainer

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal)
)

private val MonthAccent = Color(0xFF00FF66)
private val RamsSurface = Color(0xFF151619)

@Composable
fun CalendarGrid(
    days: List<CalendarDayModel>,
    onDayClick: (CalendarDayModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(bottom = Dimens.spaceBase),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXl), // bigger gap between rows
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = days,
                key = { it.date.toString() }
            ) { dayModel ->
                CalendarDayItem(
                    dayNumber = dayModel.date.dayOfMonth,
                    isWalked = dayModel.walked,
                    distanceKm = dayModel.distanceKm,
                    minutes = dayModel.minutes,
                    onClick = { onDayClick(dayModel) }
                )
            }
        }
    }
}

@Composable
fun CalendarDayItem(
    dayNumber: Int,
    isWalked: Boolean,
    distanceKm: Double,
    minutes: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp) // gap between oval and stats group
    ) {
        // Day oval
        Box(
            modifier = Modifier
                .size(Dimens.size4xl)
                .clip(CircleShape)
                .background(
                    if (isWalked) RamsSurface
                    else SurfaceContainer
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNumber.toString(),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = (-0.5).sp,
                color = if (isWalked) MonthAccent else MaterialTheme.colorScheme.onSurface,
                style = if (isWalked) {
                    androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = MonthAccent.copy(alpha = 0.2f),
                            offset = Offset.Zero,
                            blurRadius = 10f
                        )
                    )
                } else {
                    androidx.compose.ui.text.TextStyle.Default
                }
            )
        }

        // Stats group — kms and mins tightly stacked together
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (distanceKm > 0) "${"%.1f".format(distanceKm)}km" else "—",
                fontFamily = VictorMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = if (minutes > 0) "${minutes}m" else "—",
                fontFamily = VictorMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}