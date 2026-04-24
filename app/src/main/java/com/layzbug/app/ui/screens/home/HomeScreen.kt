package com.layzbug.app.ui.screens.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.layzbug.app.R
import com.layzbug.app.ui.components.MonthHero
import com.layzbug.app.ui.components.YearlyStatsWithDropdown
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.StrokeCap

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

@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToMonthDetail: () -> Unit,
    isLoggedIn: Boolean = true,
    onSignInSuccess: () -> Unit = {}
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val yearlyWalks by viewModel.yearlyWalks.collectAsState()
    val currentMonthWalks by viewModel.currentMonthWalks.collectAsState()

    val bannerVisible = !isLoggedIn
    var showSyncInfoSheet by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    scope.launch {
                        val authManager = com.layzbug.app.data.auth.AuthManager(context)
                        authManager.signInWithGoogle(account)
                        viewModel.onUserSignedIn()
                        onSignInSuccess()
                    }
                } catch (e: ApiException) {
                    // Handle error
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceColor)
                .padding(horizontal = Dimens.spaceBase)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = bannerVisible,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + androidx.compose.animation.fadeOut()
                ) {
                    GoogleSignInCard(
                        onClick = { showSyncInfoSheet = true }
                    )
                }

                YearlyStatsWithDropdown(
                    totalWalks = yearlyWalks.value,
                    totalDistanceKm = yearlyWalks.distanceKm,
                    totalMinutes = yearlyWalks.totalMinutes,
                    selectedYear = LocalDate.now().year,
                    showDropdown = false,
                    onClick = onNavigateToHistory,
                    modifier = Modifier.fillMaxWidth()
                )

                MonthHero(
                    stats = currentMonthWalks,
                    modifier = Modifier.fillMaxWidth().graphicsLayer(),
                    onClick = onNavigateToMonthDetail
                )
            }
        }

        // Bottom sheet scrim
        if (showSyncInfoSheet) {
            SyncInfoBottomSheet(
                onClose = { showSyncInfoSheet = false },
                onSignInClick = {
                    showSyncInfoSheet = false
                    val authManager = com.layzbug.app.data.auth.AuthManager(context)
                    val signInIntent = authManager.getGoogleSignInClient().signInIntent
                    signInLauncher.launch(signInIntent)
                }
            )
        }
    }
}

@Composable
private fun GoogleSignInCard(
    onClick: () -> Unit
) {
    val ramsSurface = Color(0xFF151619)
    val ramsBorder = Color.White.copy(alpha = 0.05f)
    val ramsTextMuted = Color.White.copy(alpha = 0.6f)
    val ramsGridLine = Color.Gray.copy(alpha = 0.03f)
    val ramsChipBg = Color.White.copy(alpha = 0.03f)
    val orangeAccent = Color(0xFFFF4400)

    val victorMono = FontFamily(
        Font(R.font.victor_mono_regular, FontWeight.Normal),
        Font(R.font.victor_mono_bold, FontWeight.Bold)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(ramsSurface)
            .border(1.dp, ramsBorder, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .drawBehind {
                val gridSize = 4.dp.toPx()
                for (x in 0..size.width.toInt() step gridSize.toInt()) {
                    drawLine(
                        ramsGridLine,
                        Offset(x.toFloat(), 0f),
                        Offset(x.toFloat(), size.height),
                        1.dp.toPx()
                    )
                }
                for (y in 0..size.height.toInt() step gridSize.toInt()) {
                    drawLine(
                        ramsGridLine,
                        Offset(0f, y.toFloat()),
                        Offset(size.width, y.toFloat()),
                        1.dp.toPx()
                    )
                }
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status chip — left
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .background(ramsChipBg, CircleShape)
                    .border(1.dp, ramsBorder, CircleShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(orangeAccent)
                )
                Text(
                    text = "CLOUD SYNC IS OFF",
                    color = ramsTextMuted,
                    fontSize = 11.sp,
                    fontFamily = victorMono,
                    letterSpacing = 1.1.sp
                )
            }

            // Google logo — right
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SyncInfoBottomSheet(
    onClose: () -> Unit,
    onSignInClick: () -> Unit
) {
    val orangeAccent = Color(0xFFFF4400)
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)

    val victorMono = FontFamily(
        Font(R.font.victor_mono_regular, FontWeight.Normal),
        Font(R.font.victor_mono_bold, FontWeight.Bold)
    )
    val jetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
    )

    com.layzbug.app.ui.components.LayzbugBottomSheet(
        onClose = onClose,
        lightBackground = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dark chip
                Row(
                    modifier = Modifier
                        .height(28.dp)
                        .background(Color(0xFF151619), CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(orangeAccent)
                    )
                    Text(
                        text = "CLOUD SYNC IS OFF",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = victorMono,
                        letterSpacing = 1.1.sp
                    )
                }


            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Sign in to save your walks",
                color = headlineColor,
                fontSize = 18.sp,
                fontFamily = victorMono,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Layzbug shows your walks automatically on this phone. If you go for a walk without your phone or watch, you can mark that day yourself.\n\nJust note, if not signed in, these marked days may not appear on new logged in devices. Only the walks tracked automatically will show.",
                color = bodyTextMuted,
                fontSize = 15.sp,
                fontFamily = victorMono,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "SIGN IN IS OPTIONAL",
                color = orangeAccent,
                fontSize = 13.sp,
                fontFamily = jetBrainsMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF151619))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    .clickable { onSignInClick() }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign in to sync across devices",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = victorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height)
        val strokeWidth = s * 0.22f
        val arcSize = Size(s - strokeWidth, s - strokeWidth)
        val arcTopLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        val googleBlue = Color(0xFF4285F4)
        val googleRed = Color(0xFFEA4335)
        val googleYellow = Color(0xFFFBBC05)
        val googleGreen = Color(0xFF34A853)

        // Red — Top
        drawArc(
            color = googleRed,
            startAngle = 195f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Yellow — Left
        drawArc(
            color = googleYellow,
            startAngle = 135f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Green — Bottom
        drawArc(
            color = googleGreen,
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue — This covers the right side from where green ends up to the red start
        drawArc(
            color = googleBlue,
            startAngle = -15f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Blue horizontal bar — Needs to be slightly longer to "plug" into the arc
        // We extend the end slightly to ensure it overlaps the stroke of the arc
        drawLine(
            color = googleBlue,
            start = Offset(s / 2, s / 2),
            end = Offset(s - strokeWidth, s / 2),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square // Square cap helps fill the joint better than Butt
        )
    }
}