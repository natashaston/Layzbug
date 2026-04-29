package com.layzbug.app.ui.components

import android.graphics.Bitmap
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

private val RamsSurface    = Color(0xFF151619)
private val RamsBorder     = Color.Black.copy(alpha = 0.08f)
private val RamsChipBg     = Color(0xFF151619)
private val RamsChipBorder = Color.Black.copy(alpha = 0.08f)
private val BodyTextMuted  = Color.Black.copy(alpha = 0.6f)
private val HeadlineColor  = Color(0xFF151619)
private val OrangeAccent   = Color(0xFFFF4400)
private val GreenAccent    = Color(0xFF00FF66)

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
    val dateLabel = "$monthName ${date.dayOfMonth}, ${date.year}"
    val goalReached = durationMins >= 30

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var manualOverride   by remember(date) { mutableStateOf(if (!goalReached) currentStatus else false) }
    var showAlert        by remember(date) { mutableStateOf(if (!goalReached) currentStatus else false) }
    var showSharePreview by remember { mutableStateOf(false) }
    var shareBitmap      by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(manualOverride) {
        if (manualOverride) { delay(260); showAlert = true }
        else                { delay(260); showAlert = false }
    }

    // Sheet only shown once bitmap is ready — no stutter
    val bmp = shareBitmap
    if (showSharePreview && bmp != null) {
        SharePreviewBottomSheet(
            bitmap    = bmp,
            cardData  = WalkShareUtils.dailyCardData(date, durationMins, distanceKm),
            onDismiss = {
                showSharePreview = false
                shareBitmap = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 40.dp)
    ) {

        // Date chip + share icon
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .background(RamsChipBg, CircleShape)
                    .border(1.dp, RamsChipBorder, CircleShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(GreenAccent))
                Text(
                    text          = dateLabel,
                    color         = Color.White.copy(alpha = 0.6f),
                    fontSize      = 11.sp,
                    fontFamily    = VictorMono,
                    letterSpacing = 1.1.sp
                )
            }

            // Share icon — hidden for manually marked days
            // Tapping renders the bitmap first, then opens the sheet
            if (!manualOverride) {
                IconButton(
                    onClick  = {
                        scope.launch {
                            val cardData = WalkShareUtils.dailyCardData(date, durationMins, distanceKm)
                            shareBitmap      = WalkShareUtils.renderCardBitmap(context, cardData)
                            showSharePreview = true
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Share,
                        contentDescription = "Share walk",
                        tint               = Color(0xFF151619),
                        modifier           = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Metrics table
        Column(modifier = Modifier.fillMaxWidth()) {
            MetricRow("Minutes Walked",     if (durationMins > 0) "$durationMins min" else "—")
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RamsBorder))
            MetricRow("Kilometres Covered", if (distanceKm > 0) "${"%.1f".format(distanceKm)} km" else "—")
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RamsBorder))
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (goalReached) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFD6F5E3))
                    .border(1.dp, Color(0xFF7DCFA0), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "✓", color = Color(0xFF1A6E35), fontSize = 16.sp, fontFamily = JetBrainsMono)
                Text(
                    text          = "You've hit your 30-minute walking goal for this day.",
                    color         = Color(0xFF1A6E35),
                    fontSize      = 15.sp,
                    fontFamily    = VictorMono,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    lineHeight    = 24.sp
                )
            }
        } else {
            Column {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "Mark this day as walked",
                        color      = BodyTextMuted,
                        fontSize   = 15.sp,
                        fontFamily = VictorMono,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp,
                        modifier   = Modifier.weight(1f).padding(end = 16.dp)
                    )
                    Switch(
                        checked         = manualOverride,
                        onCheckedChange = { checked ->
                            manualOverride = checked
                            onManualOverrideChanged(checked)
                        },
                        colors = run {
                            val thumbColor  by animateColorAsState(if (manualOverride) GreenAccent else Color(0xFF888888), tween(300, easing = LinearEasing), "thumb")
                            val trackColor  by animateColorAsState(if (manualOverride) RamsSurface  else Color(0xFFE0E0E0), tween(300, easing = LinearEasing), "track")
                            val borderColor by animateColorAsState(if (manualOverride) Color.Transparent else Color(0xFFBBBBBB), tween(300, easing = LinearEasing), "border")
                            SwitchDefaults.colors(
                                checkedThumbColor   = thumbColor,  checkedTrackColor   = trackColor,  checkedBorderColor   = borderColor,
                                uncheckedThumbColor = thumbColor,  uncheckedTrackColor = trackColor,  uncheckedBorderColor = borderColor
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = showAlert,
                    enter   = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit    = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFDE8E0))
                                .border(1.dp, Color(0xFFF0A080), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment     = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "!", color = Color(0xFFCC3300), fontSize = 13.sp, fontFamily = JetBrainsMono)
                            Text(
                                text          = "No 30 minute walk was detected by the app on this day. Since you have confirmed that you walked, we are counting this day towards your goal.",
                                color         = Color(0xFFCC3300),
                                fontSize      = 15.sp,
                                fontFamily    = VictorMono,
                                fontWeight    = FontWeight.Medium,
                                letterSpacing = 0.sp,
                                lineHeight    = 24.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (!isLoggedIn) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(RamsBorder))
                            Spacer(modifier = Modifier.height(32.dp))

                            Row(
                                modifier = Modifier
                                    .height(28.dp)
                                    .background(RamsSurface, CircleShape)
                                    .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                                    .padding(horizontal = 12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OrangeAccent))
                                Text(text = "CLOUD SYNC IS OFF", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = VictorMono, letterSpacing = 1.1.sp)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text       = "Although you marked a day as walked/unwalked, this might not sync across other devices unless you login.",
                                color      = BodyTextMuted,
                                fontSize   = 15.sp,
                                fontFamily = VictorMono,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(CircleShape)
                                    .background(RamsSurface)
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                                    .clickable { onSignInClick() }
                                    .padding(horizontal = 20.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                GoogleGLogoInline(modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Login to sync across devices", color = Color.White, fontSize = 14.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleGLogoInline(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height)
        val sw = s * 0.22f
        val arcSize = Size(s - sw, s - sw)
        val tl = Offset(sw / 2, sw / 2)
        drawArc(Color(0xFFEA4335), 195f, 105f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Butt))
        drawArc(Color(0xFFFBBC05), 135f,  60f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Butt))
        drawArc(Color(0xFF34A853),  45f,  90f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Butt))
        drawArc(Color(0xFF4285F4), -15f, 105f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Butt))
        drawLine(Color(0xFF4285F4), Offset(s/2, s/2), Offset(s - sw, s/2), sw, StrokeCap.Square)
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = BodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
        Text(text = value, color = HeadlineColor, fontSize = 16.sp, fontFamily = JetBrainsMono, letterSpacing = (-0.5).sp)
    }
}