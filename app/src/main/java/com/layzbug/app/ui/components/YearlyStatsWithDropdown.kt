package com.layzbug.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.R

// JetBrains Mono font family — for metric numbers and dropdown
private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

// Victor Mono font family — for labels
private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

// Dieter Rams palette
private val RamsSurface = Color(0xFF151619)
private val RamsBorder = Color.White.copy(alpha = 0.05f)
private val RamsTextMuted = Color.White.copy(alpha = 0.6f)
private val RamsGridLine = Color.Gray.copy(alpha = 0.03f)
private val RamsAccent = Color(0xFFFF4400)
private val RamsDivider = Color.White.copy(alpha = 0.05f)
private val RamsChipBg = Color.White.copy(alpha = 0.03f)

@Composable
fun YearlyStatsWithDropdown(
    totalWalks: Int,
    totalDistanceKm: Double = 0.0,
    totalMinutes: Long = 0L,
    selectedYear: Int,
    availableYears: List<Int> = emptyList(),
    onYearSelected: (Int) -> Unit = {},
    showDropdown: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RamsSurface)
            .border(1.dp, RamsBorder, RoundedCornerShape(24.dp))
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
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
            // Header row: status chip + year dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill-shaped status chip
                Row(
                    modifier = Modifier
                        .height(28.dp)
                        .background(RamsChipBg, CircleShape)
                        .border(1.dp, RamsBorder, CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPulseDot()
                    Text(
                        text = "ANNUAL OVERVIEW",
                        color = RamsTextMuted,
                        fontSize = 11.sp,
                        fontFamily = VictorMono,
                        letterSpacing = 1.1.sp
                    )
                }

                // Year dropdown — only shown when showDropdown is true
                if (showDropdown) {
                    Box {
                        RamsDropdown(
                            selectedYear = selectedYear,
                            onClick = { expanded = true }
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .background(RamsSurface)
                                .border(1.dp, RamsBorder, RoundedCornerShape(8.dp))
                        ) {
                            availableYears.forEach { year ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = year.toString(),
                                            color = if (year == selectedYear) RamsAccent else RamsTextMuted,
                                            fontSize = 13.sp,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.3.sp
                                        )
                                    },
                                    onClick = {
                                        onYearSelected(year)
                                        expanded = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = RamsTextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Metrics row — 3 equal columns separated by dividers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                YearMetricColumn(
                    value = totalWalks.toString(),
                    label = "DAYS WALKED",
                    modifier = Modifier.weight(1f)
                )

                YearVerticalDivider()

                YearMetricColumn(
                    value = formatDistanceRams(totalDistanceKm),
                    label = "KMS COVERED",
                    modifier = Modifier.weight(1f)
                )

                YearVerticalDivider()

                YearMetricColumn(
                    value = totalMinutes.toString(),
                    label = "MINS WALKED",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun YearMetricColumn(
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
            color = RamsAccent,
            fontSize = 30.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-1.8).sp,
            lineHeight = 1.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = RamsAccent.copy(alpha = 0.8f),
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
private fun YearVerticalDivider() {
    Box(
        modifier = Modifier
            .padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
            .width(1.dp)
            .height(40.dp)
            .background(RamsDivider)
    )
}

@Composable
private fun RamsDropdown(selectedYear: Int, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f,
        label = "scale"
    )
    val bgColor by animateColorAsState(
        if (isPressed) Color.White.copy(alpha = 0.1f) else RamsChipBg,
        label = "bg"
    )

    Row(
        modifier = Modifier
            .height(28.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(bgColor, CircleShape)
            .border(1.dp, RamsBorder, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = selectedYear.toString(),
            color = RamsAccent,
            fontSize = 13.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(12.dp)
                .background(Color.White.copy(alpha = 0.1f))
        )

        Canvas(modifier = Modifier.size(width = 10.dp, height = 6.dp)) {
            val path = Path().apply {
                moveTo(1.5.dp.toPx(), 1.5.dp.toPx())
                lineTo(5.dp.toPx(), 5.dp.toPx())
                lineTo(8.5.dp.toPx(), 1.5.dp.toPx())
            }
            drawPath(path, RamsAccent, alpha = 0.9f, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Square))
        }
    }
}

@Composable
private fun StatusPulseDot() {
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
            .background(Color(0xFFEF4444).copy(alpha = alpha))
    )
}

private fun formatDistanceRams(km: Double): String {
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