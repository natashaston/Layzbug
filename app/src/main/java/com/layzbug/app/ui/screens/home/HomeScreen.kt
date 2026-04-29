package com.layzbug.app.ui.screens.home

import android.app.Activity
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
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal))
private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

private val RamsSurface   = Color(0xFF151619)
private val RamsBorder    = Color.White.copy(alpha = 0.05f)
private val RamsTextMuted = Color.White.copy(alpha = 0.6f)
private val RamsGridLine  = Color.Gray.copy(alpha = 0.03f)
private val RamsChipBg    = Color.White.copy(alpha = 0.03f)
private val OrangeAccent  = Color(0xFFFF4400)
private val GreenAccent   = Color(0xFF00FF66)

@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToMonthDetail: () -> Unit,
    isLoggedIn: Boolean = true,
    onSignInSuccess: () -> Unit = {}
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val yearlyWalks       by viewModel.yearlyWalks.collectAsState()
    val currentMonthWalks by viewModel.currentMonthWalks.collectAsState()
    val isSyncing         by viewModel.isSyncing.collectAsState()
    val syncCompleted     by viewModel.syncCompleted.collectAsState()

    var showSyncInfoSheet      by remember { mutableStateOf(false) }
    var showSuccessToast       by remember { mutableStateOf(false) }
    var showSignedInInfoSheet  by remember { mutableStateOf(false) }
    var showSyncInfoPopup      by remember { mutableStateOf(false) }
    val syncToastDismissed     by viewModel.syncToastDismissed.collectAsState()

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
                        showSuccessToast = true
                    }
                } catch (e: ApiException) { /* handle */ }
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
                AnimatedVisibility(
                    visible = (isSyncing || syncCompleted) && !syncToastDismissed,
                    enter = expandVertically(tween(300)) + fadeIn(),
                    exit  = shrinkVertically(tween(300)) + fadeOut()
                ) {
                    SyncProgressToast(
                        isSyncing = isSyncing,
                        onDismiss = { viewModel.dismissSyncToast() },
                        onInfo    = { showSyncInfoPopup = true }
                    )
                }

                AnimatedVisibility(
                    visible = !isLoggedIn || showSuccessToast,
                    enter = expandVertically(tween(300)) + fadeIn(),
                    exit  = shrinkVertically(tween(300)) + fadeOut()
                ) {
                    if (!isLoggedIn) {
                        GoogleSignInCard(onClick = { showSyncInfoSheet = true })
                    } else {
                        SignedInToast(
                            onDismiss = { showSuccessToast = false },
                            onInfo = { showSignedInInfoSheet = true }
                        )
                    }
                }

                // Share logic is intentionally omitted here to hide the icons
                YearlyStatsWithDropdown(
                    totalWalks      = yearlyWalks.value,
                    totalDistanceKm = yearlyWalks.distanceKm,
                    totalMinutes    = yearlyWalks.totalMinutes,
                    selectedYear    = LocalDate.now().year,
                    showDropdown    = false,
                    onClick         = onNavigateToHistory,
                    modifier        = Modifier.fillMaxWidth()
                )

                // Share logic is intentionally omitted here to hide the icons
                MonthHero(
                    stats    = currentMonthWalks,
                    modifier = Modifier.fillMaxWidth().graphicsLayer(),
                    onClick  = onNavigateToMonthDetail
                )
            }
        }

        if (showSyncInfoSheet) {
            SyncInfoBottomSheet(
                onClose = { showSyncInfoSheet = false },
                onSignInClick = {
                    showSyncInfoSheet = false
                    val authManager = com.layzbug.app.data.auth.AuthManager(context)
                    signInLauncher.launch(authManager.getGoogleSignInClient().signInIntent)
                }
            )
        }

        if (showSignedInInfoSheet) {
            HomeSignedInInfoSheet(onClose = { showSignedInInfoSheet = false })
        }

        if (showSyncInfoPopup) {
            SyncStatusInfoSheet(
                isSyncing = isSyncing,
                onClose = { showSyncInfoPopup = false }
            )
        }
    }
}

// ─── SIGNED-IN SUCCESS TOAST ─────────────────────────────────────────

@Composable
private fun SignedInToast(
    onDismiss: () -> Unit,
    onInfo: () -> Unit
) {
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

            Text(
                text = "Logged in.",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .height(28.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onInfo() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    color = Color(0xFF1A6E35),
                    fontSize = 16.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

// ─── LOGIN CARD ──────────────────────────────────────────────────────

@Composable
private fun GoogleSignInCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OrangeAccent))
                Text(
                    text = "CLOUD SYNC IS OFF",
                    color = RamsTextMuted,
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    letterSpacing = 1.1.sp
                )
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── SYNC INFO SHEET ─────────────────────────────────────────────────

@Composable
private fun SyncInfoBottomSheet(onClose: () -> Unit, onSignInClick: () -> Unit) {
    val orangeAccent  = Color(0xFFFF4400)
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)
    val victorMono    = FontFamily(Font(R.font.victor_mono_regular, FontWeight.Normal), Font(R.font.victor_mono_bold, FontWeight.Bold))
    val jetBrainsMono = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal))

    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onClose, lightBackground = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.height(28.dp)
                    .background(Color(0xFF151619), CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(orangeAccent))
                Text("CLOUD SYNC IS OFF", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = victorMono, letterSpacing = 1.1.sp)
            }

            Spacer(Modifier.height(32.dp))
            Text("Sign in to save your walks", color = headlineColor, fontSize = 18.sp, fontFamily = victorMono, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = (-0.3).sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Layzbug automatically tracks your 30 minute walks on this device. If you go for a walk without your phone, you can manually mark that day as walked.\n\nJust note, if not signed in, these marked days may not appear on other devices. Only the walks tracked automatically will show.",
                color = bodyTextMuted, fontSize = 15.sp, fontFamily = victorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp
            )
            Spacer(Modifier.height(28.dp))
            Text("SIGN IN IS OPTIONAL", color = orangeAccent, fontSize = 13.sp, fontFamily = jetBrainsMono, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp)
                    .clip(CircleShape).background(Color(0xFF151619))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    .clickable { onSignInClick() }.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sign in to sync across devices", color = Color.White, fontSize = 14.sp, fontFamily = victorMono, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
            }
        }
    }
}

// ─── SYNC PROGRESS TOAST ─────────────────────────────────────────────

@Composable
private fun SyncProgressToast(
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onInfo: () -> Unit
) {
    val SyncBlue   = Color(0xFF1A56A0)
    val SyncBlueLight = Color(0xFF2A76D0)
    val SyncGreen  = Color(0xFF1A6E35)
    val SyncGreenLight = Color(0xFF2A9E50)

    val bgColor    = if (isSyncing) SyncBlue   else SyncGreen
    val borderColor = if (isSyncing) SyncBlueLight else SyncGreenLight
    val statusText = if (isSyncing) "Syncing your walks..." else "Synced successfully."

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(36.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSyncing) {
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

            Text(
                text = statusText,
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .height(28.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onInfo() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    color = bgColor,
                    fontSize = 16.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

// ─── SYNC STATUS INFO SHEET ───────────────────────────────────────────

@Composable
private fun SyncStatusInfoSheet(
    isSyncing: Boolean,
    onClose: () -> Unit
) {
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)
    val SyncBlue     = Color(0xFF1A56A0)

    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onClose, lightBackground = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 40.dp)
        ) {
            val chipBg   = if (isSyncing) SyncBlue else Color(0xFF1A6E35)
            val chipText = if (isSyncing) "SYNCING IN PROGRESS" else "SYNC COMPLETE"
            val dotColor = if (isSyncing) Color(0xFF90C8FF) else GreenAccent

            Row(
                modifier = Modifier
                    .height(28.dp)
                    .background(chipBg, CircleShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = chipText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    letterSpacing = 1.1.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = if (isSyncing) "Fetching your walk history" else "Your walk history is ready",
                color = headlineColor,
                fontSize = 18.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(20.dp))

            if (isSyncing) {
                Text(
                    text = "Layzbug is reading your walk history from your phone. This can take a moment as we go through up to a year of data.",
                    color = bodyTextMuted,
                    fontSize = 15.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "You can close this alert once the sync is complete. You can use the app normally in the meantime.",
                    color = bodyTextMuted,
                    fontSize = 15.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            } else {
                Text(
                    text = "All your walk history has been fetched from your phone and is ready to view.",
                    color = bodyTextMuted,
                    fontSize = 15.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "From here on, Layzbug will sync any new walks automatically in the background.",
                    color = bodyTextMuted,
                    fontSize = 15.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

// ─── SIGNED-IN INFO SHEET ────────────────────────────────────────────

@Composable
private fun HomeSignedInInfoSheet(onClose: () -> Unit) {
    val GoalGreenText = Color(0xFF1A6E35)
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)

    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onClose, lightBackground = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .background(GoalGreenText, CircleShape)
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
                    text = "CLOUD SYNC IS ON",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    letterSpacing = 1.1.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Your walks are saved",
                color = headlineColor,
                fontSize = 18.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                letterSpacing = (-0.3).sp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Your walks are now synced across all your devices. Whenever you log in to a new device using the same Google account, all your walks would be shown.",
                color = bodyTextMuted,
                fontSize = 15.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "This includes walks that were automatically detected by the app, as well as any days you marked manually.",
                color = bodyTextMuted,
                fontSize = 15.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )
        }
    }
}

// ─── GOOGLE G LOGO ───────────────────────────────────────────────────

@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
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