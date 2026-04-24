package com.layzbug.app.ui.screens.month

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.layzbug.app.R
import com.layzbug.app.ui.components.CalendarGrid
import com.layzbug.app.ui.components.LayzbugBottomSheet
import com.layzbug.app.ui.components.EditWalkStatusContent
import com.layzbug.app.ui.components.MonthHero
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceColor
import com.layzbug.app.data.viewmodel.MonthViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDate
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDetailScreen(
    onBack: () -> Unit,
    year: Int = YearMonth.now().year,
    month: Int = YearMonth.now().monthValue,
    viewModel: MonthViewModel = hiltViewModel()
) {
    var showEditSheet      by remember { mutableStateOf(false) }
    var showSyncSheet      by remember { mutableStateOf(false) }
    var selectedDate       by remember { mutableStateOf<LocalDate?>(null) }
    var dontShowAgain      by remember { mutableStateOf(false) }

    LaunchedEffect(year, month) {
        viewModel.loadMonthData(YearMonth.of(year, month))
    }

    val walkDays          by viewModel.walkDays.collectAsState()
    val rawMonthStats     by viewModel.monthStats.collectAsState()
    val showSignInPrompt  by viewModel.showSignInPrompt.collectAsState()
    val suppressSyncSheet by viewModel.suppressSyncPrompt.collectAsState()

    val scope = rememberCoroutineScope()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    scope.launch {
                        try {
                            val success = viewModel.signInWithGoogle(account)
                            if (success) viewModel.syncAfterSignIn()
                        } catch (e: Exception) {
                            Log.e("MonthDetailScreen", "Sign-in error: ${e.message}")
                        }
                    }
                } catch (e: ApiException) {
                    Log.e("MonthDetailScreen", "ApiException: ${e.statusCode}")
                }
            }
        }
    }

    // When ViewModel signals to show prompt AND user hasn't suppressed it,
    // wait for edit sheet to be closed first then show sync sheet
    LaunchedEffect(showSignInPrompt) {
        if (showSignInPrompt && !suppressSyncSheet) {
            // Edit sheet closes first (handled in onClose below), then this fires
            showSyncSheet = true
            viewModel.dismissSignInPrompt()
        }
    }

    Scaffold(
        containerColor = SurfaceColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = paddingValues.calculateBottomPadding())
        ) {
            MonthHero(
                stats = rawMonthStats,
                isCurrentMonth = year == YearMonth.now().year && month == YearMonth.now().monthValue,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.spaceBase))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 32.dp)
                    .alpha(0.3f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Black.copy(alpha = 0.1f)))
                Text(
                    text = "DAILY BREAKDOWN",
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = Color.Black
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Black.copy(alpha = 0.1f)))
            }

            CalendarGrid(
                days = walkDays,
                onDayClick = { clickedDay ->
                    selectedDate = clickedDay.date
                    showEditSheet = true
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // ── Sheet 1: Edit walk status ────────────────────────────────────
    if (showEditSheet && selectedDate != null) {
        val walkDay = walkDays.find { it.date == selectedDate }
        var pendingOverride by remember(selectedDate) { mutableStateOf(walkDay?.walked ?: false) }

        val originalStatus = walkDay?.walked ?: false

        LayzbugBottomSheet(
            onClose = {
                selectedDate?.let {
                    viewModel.setWalkStatus(it, pendingOverride, originalStatus)
                }
                showEditSheet = false
            },
            lightBackground = true,
            showDragHandle = true
        ) {
            EditWalkStatusContent(
                date = selectedDate!!.toKotlinLocalDate(),
                currentStatus = walkDay?.walked ?: false,
                distanceKm = walkDay?.distanceKm ?: 0.0,
                durationMins = walkDay?.minutes ?: 0,
                onWalked = { },
                onNotWalked = { },
                onManualOverrideChanged = { override -> pendingOverride = override }
            )
        }
    }

    // ── Sheet 2: Sync prompt (reuses home sheet content) ────────────
    if (showSyncSheet) {
        MonthSyncPromptSheet(
            dontShowAgain = dontShowAgain,
            onDontShowAgainToggle = { dontShowAgain = it },
            onClose = {
                if (dontShowAgain) viewModel.suppressSyncPromptPermanently()
                showSyncSheet = false
                dontShowAgain = false
            },
            onSignInClick = {
                showSyncSheet = false
                viewModel.launchSignIn(signInLauncher)
            }
        )
    }
}

// ─── SYNC PROMPT SHEET ───────────────────────────────────────────────
// Reuses the exact same design as SyncInfoBottomSheet in HomeScreen,
// with an added "Don't show this again" checkbox at the bottom.

@Composable
private fun MonthSyncPromptSheet(
    dontShowAgain: Boolean,
    onDontShowAgainToggle: (Boolean) -> Unit,
    onClose: () -> Unit,
    onSignInClick: () -> Unit
) {
    val orangeAccent  = Color(0xFFFF4400)
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)
    val ramsSurface   = Color(0xFF151619)

    LayzbugBottomSheet(
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
            // ── Chip ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .background(ramsSurface, CircleShape)
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
                    fontFamily = VictorMono,
                    letterSpacing = 1.1.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Headline ─────────────────────────────────────────────
            Text(
                text = "Sign in to save your walks",
                color = headlineColor,
                fontSize = 18.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Body ─────────────────────────────────────────────────
            Text(
                text = "Although you marked a day as walked/unwalked, this might not sync across other devices unless you login.",
                color = bodyTextMuted,
                fontSize = 15.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Optional label ───────────────────────────────────────
            Text(
                text = "SIGN IN IS OPTIONAL",
                color = orangeAccent,
                fontSize = 13.sp,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Sign in button ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(ramsSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    .clickable { onSignInClick() }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogoMonth(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign in to sync across devices",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Don't show again — only on this sheet ───────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDontShowAgainToggle(!dontShowAgain) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Custom Rams-style checkbox — Canvas tick, pixel-perfect centring
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (dontShowAgain) ramsSurface else Color.Transparent
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (dontShowAgain) Color.White.copy(alpha = 0.15f)
                            else bodyTextMuted.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(5.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (dontShowAgain) {
                        Canvas(modifier = Modifier.size(11.dp)) {
                            val w = size.width
                            val h = size.height
                            val tickPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.08f, h * 0.52f)
                                lineTo(w * 0.38f, h * 0.82f)
                                lineTo(w * 0.92f, h * 0.18f)
                            }
                            drawPath(
                                path  = tickPath,
                                color = Color(0xFF00FF66),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.8.dp.toPx(),
                                    cap   = StrokeCap.Round,
                                    join  = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Text(
                    text = "Don't show this again",
                    color = bodyTextMuted,
                    fontSize = 14.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ─── GOOGLE G LOGO (local copy for this file) ───────────────────────
@Composable
private fun GoogleGLogoMonth(modifier: Modifier = Modifier) {
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