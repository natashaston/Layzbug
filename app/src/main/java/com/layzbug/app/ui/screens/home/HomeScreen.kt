package com.layzbug.app.ui.screens.home

import android.app.Activity
import androidx.compose.ui.text.style.TextAlign
import android.provider.Settings
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.layzbug.app.R
import com.layzbug.app.ui.components.MonthHero
import com.layzbug.app.ui.components.YearlyStatsWithDropdown
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceColor
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

// ─── MANUFACTURER DETECTION ──────────────────────────────────────────

private data class MfrInfo(val label: String, val packageName: String, val playStoreId: String? = null)

private fun detectMfr(): MfrInfo = when (Build.MANUFACTURER.lowercase().trim()) {
    "samsung"                   -> MfrInfo("Samsung Health",  "com.sec.android.app.shealth")
    "xiaomi", "redmi", "poco"   -> MfrInfo("Mi Fitness",      "com.xiaomi.wearable")
    "huawei", "honor"           -> MfrInfo("Huawei Health",   "", playStoreId = "nl.appyhapps.healthsync")
    "oneplus", "oppo", "realme" -> MfrInfo("OHealth",         "com.oppo.health")
    else                        -> MfrInfo("Google Fit",      "com.google.android.apps.fitness")
}

private fun openMfrApp(context: Context, info: MfrInfo) {
    try {
        if (info.playStoreId != null) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${info.playStoreId}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (info.packageName == "com.google.android.apps.fitness") {
            try {
                context.startActivity(Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) { openAppSettings(context) }
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(info.packageName)
        if (intent != null) context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${info.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { }
}

private fun openAppSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

// ─── HOME SCREEN ─────────────────────────────────────────────────────

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
    val mfr     = remember { detectMfr() }

    val yearlyWalks        by viewModel.yearlyWalks.collectAsState()
    val currentMonthWalks  by viewModel.currentMonthWalks.collectAsState()
    val isSyncing          by viewModel.isSyncing.collectAsState()
    val syncCompleted      by viewModel.syncCompleted.collectAsState()
    val syncProgress       by viewModel.syncProgress.collectAsState()
    val fitnessConnected   by viewModel.fitnessConnected.collectAsState()
    val syncToastDismissed by viewModel.syncToastDismissed.collectAsState()

    var showSyncInfoSheet        by remember { mutableStateOf(false) }
    var showSuccessToast         by remember { mutableStateOf(false) }
    var showSignedInInfoSheet    by remember { mutableStateOf(false) }
    var showSyncInfoPopup        by remember { mutableStateOf(false) }
    var showDataSourceInfoSheet  by remember { mutableStateOf(false) }
    var dataSourceToastDismissed by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncTodayIfNeeded()
                dataSourceToastDismissed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                scope.launch {
                    com.layzbug.app.data.auth.AuthManager(context).signInWithGoogle(account)
                    viewModel.onUserSignedIn()
                    onSignInSuccess()
                    showSuccessToast = true
                }
            } catch (e: ApiException) { }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(SurfaceColor).padding(horizontal = Dimens.spaceBase)) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Sync progress toast ──────────────────────────────
                AnimatedVisibility(
                    visible = (isSyncing || syncCompleted) && !syncToastDismissed,
                    enter   = expandVertically(tween(300)) + fadeIn(),
                    exit    = shrinkVertically(tween(300)) + fadeOut()
                ) {
                    SyncProgressToast(
                        isSyncing    = isSyncing,
                        syncProgress = syncProgress,
                        onDismiss    = { viewModel.dismissSyncToast() },
                        onInfo       = { showSyncInfoPopup = true }
                    )
                }

                // ── Data source not connected toast ──────────────────
                AnimatedVisibility(
                    visible = fitnessConnected == false && !dataSourceToastDismissed,
                    enter   = expandVertically(tween(300)) + fadeIn(),
                    exit    = shrinkVertically(tween(300)) + fadeOut()
                ) {
                    DataSourceToast(
                        mfrLabel  = mfr.label,
                        onDismiss = { dataSourceToastDismissed = true },
                        onInfo    = { showDataSourceInfoSheet = true }
                    )
                }

                // ── Sign in card / signed-in toast ───────────────────
                AnimatedVisibility(
                    visible = !isLoggedIn || showSuccessToast,
                    enter   = expandVertically(tween(300)) + fadeIn(),
                    exit    = shrinkVertically(tween(300)) + fadeOut()
                ) {
                    if (!isLoggedIn) {
                        GoogleSignInCard(onClick = { showSyncInfoSheet = true })
                    } else {
                        SignedInToast(
                            onDismiss = { showSuccessToast = false },
                            onInfo    = { showSignedInInfoSheet = true }
                        )
                    }
                }

                YearlyStatsWithDropdown(
                    totalWalks      = yearlyWalks.value,
                    totalDistanceKm = yearlyWalks.distanceKm,
                    totalMinutes    = yearlyWalks.totalMinutes,
                    selectedYear    = LocalDate.now().year,
                    showDropdown    = false,
                    onClick         = onNavigateToHistory,
                    modifier        = Modifier.fillMaxWidth()
                )

                MonthHero(
                    stats    = currentMonthWalks,
                    modifier = Modifier.fillMaxWidth().graphicsLayer(),
                    onClick  = onNavigateToMonthDetail
                )
            }
        }

        if (showSyncInfoSheet) {
            SyncInfoBottomSheet(
                onClose       = { showSyncInfoSheet = false },
                onSignInClick = {
                    showSyncInfoSheet = false
                    signInLauncher.launch(com.layzbug.app.data.auth.AuthManager(context).getGoogleSignInClient().signInIntent)
                }
            )
        }
        if (showSignedInInfoSheet) HomeSignedInInfoSheet(onClose = { showSignedInInfoSheet = false })
        if (showSyncInfoPopup) SyncStatusInfoSheet(isSyncing = isSyncing, onClose = { showSyncInfoPopup = false })
        if (showDataSourceInfoSheet) {
            DataSourceInfoSheet(
                mfrLabel  = mfr.label,
                onClose   = { showDataSourceInfoSheet = false },
                onConnect = { showDataSourceInfoSheet = false; openMfrApp(context, mfr) }
            )
        }
    }
}

// ─── SYNC PROGRESS TOAST ─────────────────────────────────────────────
// Blue while syncing — shows rolling percentage beside the text.
// Green when done — shows dismiss X on left.

@Composable
fun SyncProgressToast(
    isSyncing: Boolean,
    syncProgress: Float = 1f,
    onDismiss: () -> Unit,
    onInfo: () -> Unit
) {
    val SyncBlue       = Color(0xFF1A56A0)
    val SyncBlueLight  = Color(0xFF2A76D0)
    val SyncGreen      = Color(0xFF1A6E35)
    val SyncGreenLight = Color(0xFF2A9E50)
    val bgColor        = if (isSyncing) SyncBlue else SyncGreen
    val borderColor    = if (isSyncing) SyncBlueLight else SyncGreenLight

    // Animate the percentage number smoothly
    val animatedProgress by animateFloatAsState(
        targetValue   = syncProgress,
        animationSpec = tween(durationMillis = 300),
        label         = "syncProgress"
    )
    val percent = (animatedProgress * 100).toInt().coerceIn(0, 100)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(36.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // Close icon — only visible when sync is done (green state)
            if (!isSyncing) {
                Box(
                    modifier         = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier         = Modifier.weight(1f).padding(horizontal = 10.dp).height(28.dp).wrapContentHeight(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text          = if (isSyncing) "Syncing your walks..." else "Synced successfully.",
                    color         = Color.White,
                    fontSize      = 11.sp,
                    fontFamily    = VictorMono,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    maxLines      = 1
                )
                // Percentage — only shown while syncing
                if (isSyncing) {
                    Text(
                        text          = "$percent%",
                        color         = Color.White.copy(alpha = 0.75f),
                        fontSize      = 11.sp,
                        fontFamily    = VictorMono,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        maxLines      = 1
                    )
                }
            }

            Box(
                modifier         = Modifier.size(36.dp).clip(CircleShape).background(Color.White).clickable { onInfo() },
                contentAlignment = Alignment.Center
            ) {
                Text("i", color = bgColor, fontSize = 16.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
            }
        }
    }
}

// ─── DATA SOURCE TOAST ───────────────────────────────────────────────

@Composable
private fun DataSourceToast(mfrLabel: String, onDismiss: () -> Unit, onInfo: () -> Unit) {
    val ToastRed      = Color(0xFF8B1A1A)
    val ToastRedLight = Color(0xFFB03030)
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(ToastRed).border(1.dp, ToastRedLight.copy(alpha = 0.4f), RoundedCornerShape(36.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text("$mfrLabel not connected.", color = Color.White, fontSize = 11.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 12.dp).height(28.dp).wrapContentHeight(Alignment.CenterVertically))
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White).clickable { onInfo() }, contentAlignment = Alignment.Center) {
                Text("i", color = ToastRed, fontSize = 16.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
            }
        }
    }
}

// ─── DATA SOURCE INFO SHEET ───────────────────────────────────────────

@Composable
private fun DataSourceInfoSheet(mfrLabel: String, onClose: () -> Unit, onConnect: () -> Unit) {
    val ToastRed      = Color(0xFF8B1A1A)
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)
    val StepGreen     = Color(0xFF1A6E35)

    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onClose, lightBackground = true) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 40.dp).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.height(28.dp).background(ToastRed, CircleShape).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFFF6B6B)))
                Text("DATA SOURCE NOT CONNECTED", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = VictorMono, letterSpacing = 1.1.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text("Connect $mfrLabel to Layzbug", color = headlineColor, fontSize = 18.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = (-0.3).sp)
            Spacer(Modifier.height(20.dp))
            Text("Layzbug reads walk data through Health Connect. Follow these steps to connect:", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                InstructionStep(number = "1", text = "Open Health Connect\n→ App access\n→ Layzbug App\n→ Tap on Allow all", green = StepGreen)
                InstructionStep(number = "2", text = "On the same screen\n→ Additional access\n→ Enable Access past data", green = StepGreen)
                InstructionStep(number = "3", text = "Go back to App access\n→ Find $mfrLabel\n→ Tap Allow all", green = StepGreen)
                InstructionStep(number = "4", text = "Return to Layzbug App", green = StepGreen)
            }
            Spacer(Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(CircleShape).background(RamsSurface).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape).clickable { onConnect() }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("Connect $mfrLabel", color = OrangeAccent, fontSize = 14.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
            }
        }
    }
}

// ─── INSTRUCTION STEP ────────────────────────────────────────────────

@Composable
private fun InstructionStep(number: String, text: String, green: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.padding(top = 4.dp).size(22.dp).clip(CircleShape).background(green), contentAlignment = Alignment.Center) {
            Text(text = number, color = Color.White, fontSize = 11.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 11.sp)
        }
        Text(text = text, color = Color.Black.copy(alpha = 0.6f), fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp, modifier = Modifier.weight(1f))
    }
}

// ─── SIGNED-IN TOAST ─────────────────────────────────────────────────

@Composable
private fun SignedInToast(onDismiss: () -> Unit, onInfo: () -> Unit) {
    val ToastGreen      = Color(0xFF1A6E35)
    val ToastGreenLight = Color(0xFF2A9E50)
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(ToastGreen).border(1.dp, ToastGreenLight.copy(alpha = 0.4f), RoundedCornerShape(36.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text("Logged in.", color = Color.White, fontSize = 11.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 12.dp).height(28.dp).wrapContentHeight(Alignment.CenterVertically))
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White).clickable { onInfo() }, contentAlignment = Alignment.Center) {
                Text("i", color = ToastGreen, fontSize = 16.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
            }
        }
    }
}

// ─── GOOGLE SIGN IN CARD ─────────────────────────────────────────────

@Composable
private fun GoogleSignInCard(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(RamsSurface).border(1.dp, RamsBorder, RoundedCornerShape(24.dp)).clickable { onClick() }
        .drawBehind {
            val g = 4.dp.toPx()
            for (x in 0..size.width.toInt() step g.toInt()) drawLine(RamsGridLine, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.dp.toPx())
            for (y in 0..size.height.toInt() step g.toInt()) drawLine(RamsGridLine, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
        }.padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.height(28.dp).background(RamsChipBg, CircleShape).border(1.dp, RamsBorder, CircleShape).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OrangeAccent))
                Text("CLOUD SYNC IS OFF", color = RamsTextMuted, fontSize = 11.sp, fontFamily = VictorMono, letterSpacing = 1.1.sp)
            }
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                GoogleGLogo(modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── SYNC INFO BOTTOM SHEET ──────────────────────────────────────────

@Composable
private fun SyncInfoBottomSheet(onClose: () -> Unit, onSignInClick: () -> Unit) {
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)
    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onClose, lightBackground = true) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 40.dp).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.height(28.dp).background(RamsSurface, CircleShape).border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OrangeAccent))
                Text("CLOUD SYNC IS OFF", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = VictorMono, letterSpacing = 1.1.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text("Sign in to save your walks", color = headlineColor, fontSize = 18.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = (-0.3).sp)
            Spacer(Modifier.height(20.dp))
            Text("Layzbug automatically tracks your 30 minute walks on this device. If you go for a walk without your phone, you can manually mark that day as walked.\n\nJust note, if not signed in, these marked days may not appear on other devices. Only the walks tracked automatically will show.", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            Spacer(Modifier.height(28.dp))
            Text("SIGN IN IS OPTIONAL", color = OrangeAccent, fontSize = 13.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(CircleShape).background(RamsSurface).border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape).clickable { onSignInClick() }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                GoogleGLogo(modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sign in to sync across devices", color = Color.White, fontSize = 14.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
            }
        }
    }
}

// ─── SYNC STATUS INFO SHEET ───────────────────────────────────────────

@Composable
fun SyncStatusInfoSheet(isSyncing: Boolean, onClose: () -> Unit) {
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)
    val SyncBlue      = Color(0xFF1A56A0)
    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onClose, lightBackground = true) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 40.dp)) {
            val chipBg   = if (isSyncing) SyncBlue else Color(0xFF1A6E35)
            val chipText = if (isSyncing) "SYNCING IN PROGRESS" else "SYNC COMPLETE"
            val dotColor = if (isSyncing) Color(0xFF90C8FF) else GreenAccent
            Row(modifier = Modifier.height(28.dp).background(chipBg, CircleShape).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(dotColor))
                Text(chipText, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = VictorMono, letterSpacing = 1.1.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text(if (isSyncing) "Fetching your walk history" else "Your walk history is ready", color = headlineColor, fontSize = 18.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = (-0.3).sp)
            Spacer(Modifier.height(20.dp))
            if (isSyncing) {
                Text("Layzbug is reading your walk history from your phone. This can take a moment as we go through up to a year of data.", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
                Spacer(Modifier.height(20.dp))
                Text("You can close this alert once the sync is complete. You can use the app normally in the meantime.", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            } else {
                Text("All your walk history has been fetched from your phone and is ready to view.", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
                Spacer(Modifier.height(20.dp))
                Text("From here on, Layzbug will sync any new walks automatically in the background.", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            }
        }
    }
}

// ─── HOME SIGNED IN INFO SHEET ────────────────────────────────────────

@Composable
private fun HomeSignedInInfoSheet(onClose: () -> Unit) {
    val GoalGreenText = Color(0xFF1A6E35)
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)
    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onClose, lightBackground = true) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 40.dp)) {
            Row(modifier = Modifier.height(28.dp).background(GoalGreenText, CircleShape).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(GreenAccent))
                Text("CLOUD SYNC IS ON", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = VictorMono, letterSpacing = 1.1.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text("Your walks are saved", color = headlineColor, fontSize = 18.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = (-0.3).sp)
            Spacer(Modifier.height(20.dp))
            Text("Your walks are now synced across all your devices. Whenever you log in to a new device using the same Google account, all your walks would be shown.", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            Spacer(Modifier.height(20.dp))
            Text("This includes walks that were automatically detected by the app, as well as any days you marked manually.", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
        }
    }
}

// ─── GOOGLE G LOGO ───────────────────────────────────────────────────

@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height); val strokeWidth = s * 0.22f
        val arcSize = Size(s - strokeWidth, s - strokeWidth); val arcTopLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val googleBlue = Color(0xFF4285F4); val googleRed = Color(0xFFEA4335); val googleYellow = Color(0xFFFBBC05); val googleGreen = Color(0xFF34A853)
        drawArc(color = googleRed,    startAngle = 195f, sweepAngle = 105f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawArc(color = googleYellow, startAngle = 135f, sweepAngle = 60f,  useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawArc(color = googleGreen,  startAngle = 45f,  sweepAngle = 90f,  useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawArc(color = googleBlue,   startAngle = -15f, sweepAngle = 105f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawLine(color = googleBlue, start = Offset(s / 2, s / 2), end = Offset(s - strokeWidth, s / 2), strokeWidth = strokeWidth, cap = StrokeCap.Square)
    }
}