package com.layzbug.app.ui.screens.month

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.layzbug.app.ui.components.EditWalkStatusContent
import com.layzbug.app.ui.components.LayzbugBottomSheet
import com.layzbug.app.ui.components.MonthHero
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceColor
import com.layzbug.app.data.viewmodel.MonthViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.toKotlinLocalDate
import java.time.LocalDate
import java.time.YearMonth

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
    viewModel: MonthViewModel = hiltViewModel(),
    onSignInSuccess: () -> Unit = {}
) {
    var showEditSheet     by remember { mutableStateOf(false) }
    var showSignedInToast by remember { mutableStateOf(false) }
    var selectedDate      by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(year, month) {
        viewModel.loadMonthData(YearMonth.of(year, month))
    }

    val walkDays       by viewModel.walkDays.collectAsState()
    val rawMonthStats  by viewModel.monthStats.collectAsState()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()

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
                            if (success) {
                                viewModel.syncAfterSignIn()
                                onSignInSuccess()
                                showSignedInToast = true
                            }
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

    Scaffold(containerColor = SurfaceColor) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = paddingValues.calculateBottomPadding())
        ) {
            // ── Signed-in toast — pushes all content down ────────────
            AnimatedVisibility(
                visible = showSignedInToast,
                enter = expandVertically(tween(300)) + fadeIn(),
                exit  = shrinkVertically(tween(300)) + fadeOut()
            ) {
                Column {
                    MonthSignedInToast(onDismiss = { showSignedInToast = false })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

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

    // ── Edit walk status sheet ───────────────────────────────────────
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
                onManualOverrideChanged = { override -> pendingOverride = override },
                isLoggedIn = isUserLoggedIn,
                onSignInClick = { viewModel.launchSignIn(signInLauncher) }
            )
        }
    }
}

// ─── MONTH SIGNED-IN TOAST ───────────────────────────────────────────

@Composable
private fun MonthSignedInToast(onDismiss: () -> Unit) {
    val ToastGreen      = Color(0xFF1A6E35)
    val ToastGreenLight = Color(0xFF2A9E50)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(ToastGreen)
            .border(1.dp, ToastGreenLight.copy(alpha = 0.4f), RoundedCornerShape(36.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Logged in. Walks now sync across all devices",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── GOOGLE G LOGO ───────────────────────────────────────────────────

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