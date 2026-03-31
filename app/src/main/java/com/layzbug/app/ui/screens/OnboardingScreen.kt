package com.layzbug.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.R

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

private val RamsSurface = Color(0xFF151619)
private val RamsBorder = Color.White.copy(alpha = 0.05f)
private val RamsTextMuted = Color.White.copy(alpha = 0.6f)
private val RamsGridLine = Color.Gray.copy(alpha = 0.03f)
private val RamsChipBg = Color.White.copy(alpha = 0.03f)
private val OrangeAccent = Color(0xFFFF4400)
private val GreenAccent = Color(0xFF00FF66)
private val RedAccent = Color(0xFFEF4444)

// ─── MAIN SCREEN ────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Page indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(totalPages) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(
                            width = if (index == currentPage) 24.dp else 6.dp,
                            height = 6.dp
                        )
                        .clip(if (index == currentPage) RoundedCornerShape(3.dp) else CircleShape)
                        .background(
                            if (index == currentPage) OrangeAccent
                            else Color.Black.copy(alpha = 0.1f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Content
        AnimatedContent(
            targetState = currentPage,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(200))
            },
            label = "onboarding"
        ) { page ->
            when (page) {
                0 -> PageHook()
                1 -> PageSmartDetection()
                2 -> PageHowItWorks()
                3 -> PageCalendarPreview()
                4 -> PageNotification()
                5 -> PagePermissions()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom nav — BACK + NEXT (oval buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button (hidden on first page)
            if (currentPage > 0) {
                OutlinedButton(
                    onClick = { currentPage-- },
                    shape = CircleShape,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.Black.copy(alpha = 0.1f))
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Black.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = "BACK",
                        fontSize = 12.sp,
                        fontFamily = VictorMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Next / Get Started
            Button(
                onClick = {
                    if (currentPage < totalPages - 1) {
                        currentPage++
                    } else {
                        onComplete()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RamsSurface
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = if (currentPage < totalPages - 1) "NEXT" else "GET STARTED",
                    color = OrangeAccent,
                    fontSize = 13.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp
                )
            }
        }
    }
}

// ─── PAGE 1: THE HOOK + CORE RULE ───────────────────────────────────

@Composable
private fun PageHook() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RamsCard {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "WHO recommends",
                    color = RamsTextMuted,
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    letterSpacing = 1.1.sp
                )
                Text(
                    text = "30",
                    color = OrangeAccent,
                    fontSize = 72.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-3).sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = OrangeAccent.copy(alpha = 0.8f),
                            offset = Offset.Zero,
                            blurRadius = 30f
                        )
                    )
                )
                Text(
                    text = "MINUTES OF WALKING DAILY",
                    color = RamsTextMuted,
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    textAlign = TextAlign.Center
                )

                // Core rule callout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Only walks over 5 minutes count.\nTotal must reach 30 minutes.",
                        color = OrangeAccent,
                        fontSize = 12.sp,
                        fontFamily = VictorMono,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Are you keeping up?",
            color = Color.Black.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Layzbug tracks it for you.\nAutomatically.",
            color = Color.Black.copy(alpha = 0.35f),
            fontSize = 13.sp,
            fontFamily = VictorMono,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// ─── PAGE 2: SMART DETECTION ────────────────────────────────────────

@Composable
private fun PageSmartDetection() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Not all steps count",
            color = Color.Black.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Counts
        RamsCard {
            Column(modifier = Modifier.padding(20.dp)) {
                ChipLabel(text = "COUNTS", color = GreenAccent)
                Spacer(modifier = Modifier.height(16.dp))
                DetectionRow(text = "A 10 min walk + a 25 min walk", isValid = true)
                Spacer(modifier = Modifier.height(8.dp))
                DetectionRow(text = "30 min continuous walk", isValid = true)
                Spacer(modifier = Modifier.height(8.dp))
                DetectionRow(text = "Walk with traffic light stops", isValid = true)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Doesn't count
        RamsCard {
            Column(modifier = Modifier.padding(20.dp)) {
                ChipLabel(text = "DOESN'T COUNT", color = RedAccent)
                Spacer(modifier = Modifier.height(16.dp))
                DetectionRow(text = "Kitchen and bathroom trips", isValid = false)
                Spacer(modifier = Modifier.height(8.dp))
                DetectionRow(text = "Walks under 5 minutes", isValid = false)
                Spacer(modifier = Modifier.height(8.dp))
                DetectionRow(text = "Moving between rooms", isValid = false)
            }
        }
    }
}

// ─── PAGE 3: HOW IT WORKS ───────────────────────────────────────────

@Composable
private fun PageHowItWorks() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "How it works",
            color = Color.Black.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureCard(
            number = "01",
            title = "AUTO SYNC",
            description = "Connects with Google Fit.\nYour walks are detected automatically."
        )
        Spacer(modifier = Modifier.height(12.dp))
        FeatureCard(
            number = "02",
            title = "TRACK EVERYTHING",
            description = "Days walked, kilometres covered.\nMonthly and yearly stats."
        )
        Spacer(modifier = Modifier.height(12.dp))
        FeatureCard(
            number = "03",
            title = "MANUAL OVERRIDE",
            description = "Left your phone at home?\nMark the day as walked manually."
        )
    }
}

// ─── PAGE 4: CALENDAR PREVIEW ───────────────────────────────────────

@Composable
private fun PageCalendarPreview() {
    val walkedDays = setOf(1,2,4,5,6,8,10,11,12,13,14,16,17,19,20,22,23,24,25)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your walking calendar",
            color = Color.Black.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        RamsCard {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ChipLabel(text = "MARCH 2026", color = GreenAccent)
                Spacer(modifier = Modifier.height(20.dp))

                // Calendar grid — 7 columns
                val days = (1..31).toList()
                val rows = days.chunked(7)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            week.forEach { day ->
                                val isWalked = walkedDays.contains(day)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isWalked) RamsSurface
                                            else Color.Black.copy(alpha = 0.04f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        letterSpacing = (-0.5).sp,
                                        color = if (isWalked) GreenAccent
                                               else Color.Black.copy(alpha = 0.3f),
                                        style = if (isWalked) {
                                            androidx.compose.ui.text.TextStyle(
                                                shadow = androidx.compose.ui.graphics.Shadow(
                                                    color = GreenAccent.copy(alpha = 0.8f),
                                                    offset = Offset.Zero,
                                                    blurRadius = 20f
                                                )
                                            )
                                        } else {
                                            androidx.compose.ui.text.TextStyle.Default
                                        }
                                    )
                                }
                            }
                            // Fill remaining cells in last row
                            repeat(7 - week.size) {
                                Spacer(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Black circle, green number = you walked.",
                    color = RamsTextMuted,
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── PAGE 5: NOTIFICATION ───────────────────────────────────────────

@Composable
private fun PageNotification() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "A gentle nudge",
            color = Color.Black.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        RamsCard {
            Column(modifier = Modifier.padding(20.dp)) {
                ChipLabel(text = "DAILY REMINDER", color = OrangeAccent)

                Spacer(modifier = Modifier.height(20.dp))

                // Mock notification
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(OrangeAccent)
                            )
                            Text(
                                text = "Layzbug",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontFamily = VictorMono,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "6:30 PM",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 10.sp,
                                fontFamily = VictorMono
                            )
                        }
                        Text(
                            text = "Hey Layzbug, no walk detected today.\nGet your ass moving! \uD83D\uDEB6",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontFamily = VictorMono,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "One notification per day. Only if\nyou haven't walked. Nothing else.",
                    color = RamsTextMuted,
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─── PAGE 6: PERMISSIONS + SIGN IN ──────────────────────────────────

@Composable
private fun PagePermissions() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Almost there",
            color = Color.Black.copy(alpha = 0.6f),
            fontSize = 16.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureCard(
            number = "01",
            title = "HEALTH CONNECT",
            description = "Grant access to read your steps\nand walking data from Google Fit."
        )
        Spacer(modifier = Modifier.height(12.dp))
        FeatureCard(
            number = "02",
            title = "HISTORICAL DATA",
            description = "Allow access to past walking data\nso we can backfill your calendar."
        )
        Spacer(modifier = Modifier.height(12.dp))
        FeatureCard(
            number = "03",
            title = "CLOUD SYNC",
            description = "Sign in with Google to sync your\nmanually marked walks across devices."
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Cloud sync is optional. The app works\nfully without signing in. You can\nsign in later from the home screen.",
            color = Color.Black.copy(alpha = 0.35f),
            fontSize = 11.sp,
            fontFamily = VictorMono,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ─── SHARED COMPONENTS ──────────────────────────────────────────────

@Composable
private fun RamsCard(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RamsSurface)
            .border(1.dp, RamsBorder, RoundedCornerShape(24.dp))
            .drawBehind {
                val gridSize = 4.dp.toPx()
                for (x in 0..size.width.toInt() step gridSize.toInt()) {
                    drawLine(RamsGridLine, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.dp.toPx())
                }
                for (y in 0..size.height.toInt() step gridSize.toInt()) {
                    drawLine(RamsGridLine, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
                }
            }
    ) {
        content()
    }
}

@Composable
private fun ChipLabel(text: String, color: Color) {
    Row(
        modifier = Modifier
            .height(24.dp)
            .background(RamsChipBg, CircleShape)
            .border(1.dp, RamsBorder, CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun DetectionRow(text: String, isValid: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isValid) "✓" else "✗",
            color = if (isValid) GreenAccent else RedAccent,
            fontSize = 14.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            color = RamsTextMuted,
            fontSize = 12.sp,
            fontFamily = VictorMono,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun FeatureCard(number: String, title: String, description: String) {
    RamsCard {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = number,
                color = OrangeAccent.copy(alpha = 0.3f),
                fontSize = 28.sp,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    color = OrangeAccent,
                    fontSize = 12.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = description,
                    color = RamsTextMuted,
                    fontSize = 12.sp,
                    fontFamily = VictorMono,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
