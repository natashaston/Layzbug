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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
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
    onManualOverrideChanged: (Boolean) -> Unit = {},
    isLoggedIn: Boolean = true,
    onSignInClick: () -> Unit = {}
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

        Spacer(modifier = Modifier.height(20.dp))

        // ── METRICS TABLE ────────────────────────────────────────────
        // Plain background — no card fill, label style matches home sheet body description
        Column(modifier = Modifier.fillMaxWidth()) {

            MetricRow(
                label = "Minutes Walked",
                value = if (durationMins > 0) "$durationMins min" else "—"
            )

            // Hairline divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RamsBorder)
            )

            MetricRow(
                label = "Kilometres Covered",
                value = if (distanceKm > 0) "${"%.1f".format(distanceKm)} km" else "—"
            )

            // Hairline divider beneath Kilometres Covered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RamsBorder)
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
                    text = "You've hit your 30-minute walking goal for this day.",
                    color = GoalGreenText,
                    fontSize = 15.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    lineHeight = 24.sp
                )
            }

        } else {

            Column {

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

                Spacer(modifier = Modifier.height(24.dp))

                // Alert + optional sign-in nudge
                AnimatedVisibility(
                    visible = showAlert,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit  = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    val AlertOrangeText    = Color(0xFFCC3300)
                    val AlertOrangeSurface = Color(0xFFFDE8E0)
                    val AlertOrangeBorder  = Color(0xFFF0A080)

                    Column {

                        // Orange alert
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
                                text = "No walk was detected by the app on this day. Since you've confirmed that you walked fo 30 minutes, we're counting this day toward your goal.",
                                color = AlertOrangeText,
                                fontSize = 15.sp,
                                fontFamily = VictorMono,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.sp,
                                lineHeight = 24.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        // Sign-in nudge — only when not logged in
                        if (!isLoggedIn) {

                            // Hairline divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(RamsBorder)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Chip — "CLOUD SYNC IS OFF"
                            Row(
                                modifier = Modifier
                                    .height(28.dp)
                                    .background(RamsSurface, CircleShape)
                                    .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(OrangeAccent)
                                )
                                Text(
                                    text = "CLOUD SYNC IS OFF",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontFamily = VictorMono,
                                    letterSpacing = 1.1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Body text
                            Text(
                                text = "Although you marked a day as walked/unwalked, this might not sync across other devices unless you login.",
                                color = BodyTextMuted,
                                fontSize = 15.sp,
                                fontFamily = VictorMono,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Sign in CTA — dark pill with Google G logo
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(CircleShape)
                                    .background(RamsSurface)
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                                    .clickable { onSignInClick() }
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                GoogleGLogoInline(modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Login to sync across devices",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = VictorMono,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── GOOGLE G LOGO ──────────────────────────────────────────────────

@Composable
private fun GoogleGLogoInline(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height)
        val strokeWidth = s * 0.22f
        val arcSize = Size(s - strokeWidth, s - strokeWidth)
        val arcTopLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val googleBlue   = Color(0xFF4285F4)
        val googleRed    = Color(0xFFEA4335)
        val googleYellow = Color(0xFFFBBC05)
        val googleGreen  = Color(0xFF34A853)
        drawArc(color = googleRed,    startAngle = 195f, sweepAngle = 105f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawArc(color = googleYellow, startAngle = 135f, sweepAngle = 60f,  useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawArc(color = googleGreen,  startAngle = 45f,  sweepAngle = 90f,  useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawArc(color = googleBlue,   startAngle = -15f, sweepAngle = 105f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawLine(color = googleBlue, start = Offset(s / 2, s / 2), end = Offset(s - strokeWidth, s / 2), strokeWidth = strokeWidth, cap = StrokeCap.Square)
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