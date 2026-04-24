package com.layzbug.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.layzbug.app.R

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

private val RamsSurface   = Color(0xFF151619)
private val RamsBorder    = Color.White.copy(alpha = 0.06f)
private val RamsTextMuted = Color.White.copy(alpha = 0.55f)
private val OrangeAccent  = Color(0xFFFF4400)
private val GreenAccent   = Color(0xFF00FF66)

@Composable
fun RamsSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(
        hostState = hostState,
        snackbar = { data -> RamsSnackbarCard(data) }
    )
}

@Composable
private fun RamsSnackbarCard(data: SnackbarData) {
    val msg       = data.visuals.message
    val isSuccess = msg.startsWith("Signed in")
    val isError   = msg.startsWith("Sign in error") ||
            msg.startsWith("Error") ||
            msg.startsWith("Sign in failed")
    val indicatorColor = if (isSuccess) GreenAccent else OrangeAccent

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor   = Color.Black.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(RamsSurface)
                .border(1.dp, RamsBorder, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // ── Top row: message + close ─────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = msg.uppercase(),
                        color = RamsTextMuted,
                        fontSize = 12.sp,
                        fontFamily = VictorMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )

                    // Close button — small dark circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, RamsBorder, CircleShape)
                            .clickable { data.dismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // ── Bottom row: big action button + indicator dot ────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Big round Google sign-in button
                    data.visuals.actionLabel?.let {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = CircleShape,
                                    ambientColor = OrangeAccent.copy(alpha = 0.3f),
                                    spotColor   = OrangeAccent.copy(alpha = 0.3f)
                                )
                                .clip(CircleShape)
                                .background(OrangeAccent)
                                .clickable { data.performAction() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // White G
                                Text(
                                    text = "G",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = "SIGN IN",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 7.sp,
                                    fontFamily = VictorMono,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Indicator dot — green if success, orange otherwise
                    // Mimics the Braun panel indicator light
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = indicatorColor.copy(alpha = 0.8f),
                                spotColor   = indicatorColor.copy(alpha = 0.8f)
                            )
                            .clip(CircleShape)
                            .background(indicatorColor)
                    )
                }
            }
        }
    }
}