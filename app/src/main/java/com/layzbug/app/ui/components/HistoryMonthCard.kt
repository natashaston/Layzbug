package com.layzbug.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

// Palette — identical to MonthHero
private val RamsSurface = Color(0xFF151619)
private val RamsBorder = Color.White.copy(alpha = 0.05f)
private val RamsGridLine = Color.Gray.copy(alpha = 0.03f)
private val RamsTextMuted = Color.White.copy(alpha = 0.6f)
private val RamsChipBg = Color.White.copy(alpha = 0.03f)
private val RamsDivider = Color.White.copy(alpha = 0.05f)
private val MonthAccent = Color(0xFF00FF66)

@Composable
fun MonthCard(
    monthName: String,
    walkCount: Int,
    distanceKm: Double = 0.0,
    isEnabled: Boolean = true,
    isCurrentMonth: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    if (!isEnabled) {
        // Disabled skeleton card
        DisabledMonthCard(monthName = monthName, modifier = modifier)
        return
    }

    // Enabled card
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
            // Header: pill chip with month name + pulse dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .height(28.dp)
                        .background(RamsChipBg, CircleShape)
                        .border(1.dp, RamsBorder, CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCurrentMonth) CardPulseDot()
                    Text(
                        text = monthName.uppercase(),
                        color = RamsTextMuted,
                        fontSize = 11.sp,
                        fontFamily = VictorMono,
                        letterSpacing = 1.1.sp,
                        maxLines = 1
                    )
                }
            }

            // Metrics: DAYS left, KMS right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Days — left aligned
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = walkCount.toString(),
                        color = MonthAccent,
                        fontSize = 32.sp,
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
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "DAYS",
                        color = RamsTextMuted,
                        fontSize = 10.sp,
                        fontFamily = VictorMono,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .width(1.dp)
                        .height(40.dp)
                        .background(RamsDivider)
                )

                // KMS — right aligned
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatDistanceCompact(distanceKm),
                        color = MonthAccent,
                        fontSize = 26.sp,
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
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "KMS",
                        color = RamsTextMuted,
                        fontSize = 10.sp,
                        fontFamily = VictorMono,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DisabledMonthCard(
    monthName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp)
            )
            .drawBehind {
                val gridSize = 4.dp.toPx()
                val gridColor = Color.Black.copy(alpha = 0.01f)
                for (x in 0..size.width.toInt() step gridSize.toInt()) {
                    drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.dp.toPx())
                }
                for (y in 0..size.height.toInt() step gridSize.toInt()) {
                    drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
                }
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = monthName.uppercase(),
                    color = Color.Black.copy(alpha = 0.2f),
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    letterSpacing = 1.1.sp
                )
            }

            // Empty spacer to match metrics area height of enabled cards
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun CardPulseDot() {
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

private fun formatDistanceCompact(km: Double): String {
    return when {
        km >= 1000 -> "%,.0f".format(km)
        km >= 10 -> Math.round(km).toString()
        km >= 0.1 -> "%.1f".format(km)
        else -> "0"
    }
}