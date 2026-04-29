package com.layzbug.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.R
import com.layzbug.app.domain.StatsValue

// Fonts
private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

// Palette
private val RamsSurface = Color(0xFF151619)
private val RamsBorder = Color.White.copy(alpha = 0.05f)
private val RamsTextMuted = Color.White.copy(alpha = 0.6f)
private val RamsGridLine = Color.Gray.copy(alpha = 0.03f)
private val MonthAccent = Color(0xFF00FF66)
private val RamsDivider = Color.White.copy(alpha = 0.05f)
private val RamsChipBg = Color.White.copy(alpha = 0.03f)

@Composable
fun MonthHero(
    stats: StatsValue,
    isCurrentMonth: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onShareClick: (() -> Unit)? = null // Made nullable to hide button completely if not provided
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RamsSurface)
            .border(1.dp, RamsBorder, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .drawBehind {
                val gridSize = 4.dp.toPx()
                for (x in 0..size.width.toInt() step gridSize.toInt()) {
                    drawLine(RamsGridLine, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.dp.toPx())
                }
                for (y in 0..size.height.toInt() step gridSize.toInt()) {
                    drawLine(RamsGridLine, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
                }
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.heightIn(min = 140.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: pill-shaped status chip and chip-styled share button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month Chip
                Row(
                    modifier = Modifier
                        .height(28.dp)
                        .background(RamsChipBg, CircleShape)
                        .border(1.dp, RamsBorder, CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCurrentMonth) MonthPulseDot()
                    Text(
                        text = stats.label.uppercase(),
                        color = RamsTextMuted,
                        fontSize = 11.sp,
                        fontFamily = VictorMono,
                        letterSpacing = 1.1.sp
                    )
                }

                // Share Button — rendered as a matching chip if lambda provided
                if (onShareClick != null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(RamsChipBg)
                            .border(1.dp, RamsBorder, CircleShape)
                            .clickable { onShareClick() }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "SHARE",
                            color = MonthAccent,
                            fontSize = 11.sp,
                            fontFamily = VictorMono,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share month stats",
                            tint = MonthAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Metrics row — 3 equal columns separated by dividers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                MetricColumn(
                    value = stats.value.toString(),
                    label = "DAYS WALKED",
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider()

                MetricColumn(
                    value = formatDistanceMonth(stats.distanceKm),
                    label = "KMS COVERED",
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider()

                MetricColumn(
                    value = stats.totalMinutes.toString(),
                    label = "MINS WALKED",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            color = MonthAccent,
            fontSize = 30.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-1.8).sp,
            lineHeight = 1.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = MonthAccent.copy(alpha = 0.8f),
                    offset = Offset.Zero,
                    blurRadius = 30f
                )
            )
        )
        Text(
            text = label,
            color = RamsTextMuted,
            fontSize = 10.sp,
            fontFamily = VictorMono,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
            .width(1.dp)
            .height(40.dp)
            .background(RamsDivider)
    )
}

@Composable
private fun MonthPulseDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(MonthAccent.copy(alpha = alpha))
    )
}

private fun formatDistanceMonth(km: Double): String {
    return if (km >= 1000) {
        "%,.0f".format(km)
    } else if (km >= 10) {
        "${Math.round(km)}"
    } else if (km >= 0.1) {
        "%.1f".format(km)
    } else {
        "0"
    }
}