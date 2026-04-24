package com.layzbug.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.R
import kotlinx.coroutines.delay
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

private val RamsSurface      = Color(0xFF151619)
private val RamsBorder       = Color.Black.copy(alpha = 0.08f)
private val RamsChipBg       = Color(0xFF151619)          // dark chip — same as home sheet
private val RamsChipBorder   = Color.Black.copy(alpha = 0.08f)
private val BodyTextMuted    = Color.Black.copy(alpha = 0.6f)   // matches home sheet description
private val HeadlineColor    = Color(0xFF151619)
private val OrangeAccent     = Color(0xFFFF4400)
private val GreenAccent      = Color(0xFF00FF66)

// ─── COMPONENT ──────────────────────────────────────────────────────

@Composable
fun EditWalkStatusContent(
    date: LocalDate,
    currentStatus: Boolean,
    distanceKm: Double,
    durationMins: Long,
    onWalked: () -> Unit,
    onNotWalked: () -> Unit,
    onManualOverrideChanged: (Boolean) -> Unit = {}
) {
    val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.uppercase()
    val day       = date.dayOfMonth.toString()
    val year      = date.year.toString()
    val dateLabel = "$monthName $day, $year"

    val goalReached = durationMins >= 30

    var manualOverride by remember(date) { mutableStateOf(if (!goalReached) currentStatus else false) }
    var showAlert      by remember(date) { mutableStateOf(if (!goalReached) currentStatus else false) }

    LaunchedEffect(manualOverride) {
        if (manualOverride) {
            delay(260)
            showAlert = true
        } else {
            delay(260)
            showAlert = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 40.dp)
    ) {

        // ── DATE CHIP ────────────────────────────────────────────────
        // Mirrors the "CLOUD SYNC IS OFF" chip from HomeScreen bottom sheet
        Row(
            modifier = Modifier
                .height(28.dp)
                .background(RamsChipBg, CircleShape)
                .border(1.dp, RamsChipBorder, CircleShape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(GreenAccent)
            )
            Text(
                text = dateLabel,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontFamily = VictorMono,
                letterSpacing = 1.1.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── HEADLINE ─────────────────────────────────────────────────
        // Matches "Sign in to save your walks" from home sheet
        Text(
            text = "Summary of your walk",
            color = HeadlineColor,
            fontSize = 18.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp,
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── METRICS TABLE ────────────────────────────────────────────
        // Plain background — no card fill, label style matches home sheet body description
        Column(modifier = Modifier.fillMaxWidth()) {

            MetricRow(
                label = "Duration Walked",
                value = if (durationMins > 0) "$durationMins minutes" else "—"
            )

            // Hairline divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RamsBorder)
            )

            MetricRow(
                label = "Distance Covered",
                value = if (distanceKm > 0) "${"%.1f".format(distanceKm)} kilometres" else "—"
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── GOAL STATE OR MANUAL OVERRIDE ────────────────────────────

        if (goalReached) {

            val GoalGreenText    = Color(0xFF1A6E35)
            val GoalGreenSurface = Color(0xFFD6F5E3)
            val GoalGreenBorder  = Color(0xFF7DCFA0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GoalGreenSurface)
                    .border(1.dp, GoalGreenBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✓",
                    color = GoalGreenText,
                    fontSize = 16.sp,
                    fontFamily = JetBrainsMono
                )
                Text(
                    text = "You've hit your 30 minute walking goal for this day.",
                    color = GoalGreenText,
                    fontSize = 15.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    lineHeight = 24.sp
                )
            }

        } else {

            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {

                // Switch row — plain background matching table style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, end = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mark this day as walked",
                        color = BodyTextMuted,
                        fontSize = 15.sp,
                        fontFamily = VictorMono,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    )
                    Switch(
                        checked = manualOverride,
                        onCheckedChange = { checked ->
                            manualOverride = checked
                            onManualOverrideChanged(checked)
                        },
                        colors = run {
                            val thumbColor by animateColorAsState(
                                targetValue = if (manualOverride) GreenAccent else Color(0xFF888888),
                                animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                                label = "switchThumb"
                            )
                            val trackColor by animateColorAsState(
                                targetValue = if (manualOverride) RamsSurface else Color(0xFFE0E0E0),
                                animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                                label = "switchTrack"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = if (manualOverride) Color.Transparent else Color(0xFFBBBBBB),
                                animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                                label = "switchBorder"
                            )
                            SwitchDefaults.colors(
                                checkedThumbColor    = thumbColor,
                                checkedTrackColor    = trackColor,
                                checkedBorderColor   = borderColor,
                                uncheckedThumbColor  = thumbColor,
                                uncheckedTrackColor  = trackColor,
                                uncheckedBorderColor = borderColor
                            )
                        }
                    )
                }

                // Alert
                AnimatedVisibility(
                    visible = showAlert,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit  = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    val AlertOrangeText    = Color(0xFFCC3300)
                    val AlertOrangeSurface = Color(0xFFFDE8E0)
                    val AlertOrangeBorder  = Color(0xFFF0A080)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AlertOrangeSurface)
                            .border(1.dp, AlertOrangeBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "!",
                            color = AlertOrangeText,
                            fontSize = 13.sp,
                            fontFamily = JetBrainsMono
                        )
                        Text(
                            text = "No walk was detected by the app on this day. Since you've confirmed that you walked, we're counting this day toward your 30 minute walking goal.",
                            color = AlertOrangeText,
                            fontSize = 15.sp,
                            fontFamily = VictorMono,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── METRIC ROW ─────────────────────────────────────────────────────
// Plain rows — no card bg — label matches home sheet body description style

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = BodyTextMuted,
            fontSize = 15.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp
        )
        Text(
            text = value,
            color = HeadlineColor,
            fontSize = 16.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.5).sp
        )
    }
}