package com.layzbug.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.R
import kotlinx.datetime.LocalDate

// ─── FONTS ──────────────────────────────────────────────────────────

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

// ─── PALETTE ────────────────────────────────────────────────────────

private val CardSurface    = Color(0xFF151619)
private val CardBorder     = Color.White.copy(alpha = 0.05f)
private val GridLine       = Color.White.copy(alpha = 0.03f)
private val OrangeAccent   = Color(0xFFFF4400)
private val GreenAccent    = Color(0xFF00FF66)
private val MutedLabel     = Color.White.copy(alpha = 0.35f)
private val MutedWatermark = Color.White.copy(alpha = 0.12f)
private val DividerColor   = Color.White.copy(alpha = 0.07f)

/**
 * ShareWalkCard
 *
 * Rendered offscreen into a Bitmap and shared via Intent.ACTION_SEND.
 * This composable is purely visual — no interaction, no state.
 *
 * Rendered at a fixed 360×220dp logical size (captured at 3× density → ~1080×660px).
 *
 * @param date         The day being shared.
 * @param durationMins Total walked minutes for the day.
 * @param distanceKm   Total walked kilometres for the day.
 */
@Composable
fun ShareWalkCard(
    date: LocalDate,
    durationMins: Long,
    distanceKm: Double,
) {
    val goalReached  = durationMins >= 30
    val accentColor  = if (goalReached) GreenAccent else OrangeAccent
    val glowBrush    = Brush.radialGradient(
        colors = listOf(accentColor.copy(alpha = 0.18f), Color.Transparent),
        radius = 340f
    )

    val monthName = date.month.name
        .lowercase()
        .replaceFirstChar { it.uppercase() }
        .uppercase()
    val dateLabel = "$monthName ${date.dayOfMonth}, ${date.year}"

    Box(
        modifier = Modifier
            .width(360.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
    ) {

        // ── Background grid texture ──────────────────────────────────
        GridOverlay()

        // ── Accent glow (top-left origin) ────────────────────────────
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .background(glowBrush, CircleShape)
        )

        // ── Content ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Top row: date chip + accent dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Accent dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )

                // Date chip
                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateLabel,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.sp,
                        fontFamily = VictorMono,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            // Middle: main stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // Minutes stat
                StatBlock(
                    value    = if (durationMins > 0) "$durationMins" else "—",
                    unit     = "MIN",
                    accent   = accentColor,
                    modifier = Modifier.weight(1f)
                )

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(DividerColor)
                        .align(Alignment.CenterVertically)
                )

                // Kilometres stat
                StatBlock(
                    value    = if (distanceKm > 0.0) "${"%.1f".format(distanceKm)}" else "—",
                    unit     = "KM",
                    accent   = accentColor,
                    modifier = Modifier.weight(1f),
                    alignEnd = true
                )
            }

            // Bottom row: goal badge OR sub-goal note + watermark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (goalReached) {
                    GoalBadge(accentColor = accentColor)
                } else {
                    // Sub-30 min: show a neutral label
                    Text(
                        text = "tracked walk",
                        color = MutedLabel,
                        fontSize = 10.sp,
                        fontFamily = VictorMono,
                        letterSpacing = 1.1.sp
                    )
                }

                // Watermark
                Text(
                    text = "LAYZBUG",
                    color = MutedWatermark,
                    fontSize = 10.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

// ─── STAT BLOCK ─────────────────────────────────────────────────────

@Composable
private fun StatBlock(
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    val alignment = if (alignEnd) Alignment.End else Alignment.Start
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 44.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-2).sp,
            lineHeight = 44.sp
        )
        Text(
            text = unit,
            color = accent.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

// ─── GOAL BADGE ─────────────────────────────────────────────────────

@Composable
private fun GoalBadge(accentColor: Color) {
    Row(
        modifier = Modifier
            .height(24.dp)
            .background(accentColor.copy(alpha = 0.12f), CircleShape)
            .border(1.dp, accentColor.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "✓",
            color = accentColor,
            fontSize = 9.sp,
            fontFamily = JetBrainsMono
        )
        Text(
            text = "30-MIN GOAL HIT",
            color = accentColor.copy(alpha = 0.85f),
            fontSize = 9.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

// ─── GRID OVERLAY ───────────────────────────────────────────────────

@Composable
private fun GridOverlay() {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val step = 20.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(GridLine, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 0.5f)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(GridLine, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 0.5f)
            y += step
        }
    }
}
