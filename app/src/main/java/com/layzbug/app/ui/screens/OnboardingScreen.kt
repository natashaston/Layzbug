package com.layzbug.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.layzbug.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

// ─── FONTS ──────────────────────────────────────────────────────────

private val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal))
private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

// ─── PALETTE ────────────────────────────────────────────────────────

private val RamsSurface   = Color(0xFF151619)
private val RamsBorder    = Color.White.copy(alpha = 0.05f)
private val RamsTextMuted = Color.White.copy(alpha = 0.6f)
private val RamsGridLine  = Color.Gray.copy(alpha = 0.03f)
private val RamsChipBg    = Color.White.copy(alpha = 0.03f)
private val OrangeAccent  = Color(0xFFFF4400)
private val GreenAccent   = Color(0xFF00FF66)
private val GoalGreen     = Color(0xFF1A6E35)

// ─── PERMISSION ROW STATE ────────────────────────────────────────────

private enum class PermRowState { IDLE, LOADING, GRANTED, FAILED }

// ─── MANUFACTURER DETECTION ──────────────────────────────────────────

private data class ManufacturerInfo(
    val label: String,
    val packageName: String,
    val playStoreId: String? = null
)

private fun detectManufacturer(): ManufacturerInfo {
    return when (Build.MANUFACTURER.lowercase().trim()) {
        "samsung"                   -> ManufacturerInfo("Samsung Health",  "com.sec.android.app.shealth")
        "xiaomi", "redmi", "poco"   -> ManufacturerInfo("Mi Fitness",      "com.xiaomi.wearable")
        "huawei", "honor"           -> ManufacturerInfo("Huawei Health",   "", playStoreId = "nl.appyhapps.healthsync")
        "oneplus", "oppo", "realme" -> ManufacturerInfo("OHealth",         "com.oppo.health")
        else                        -> ManufacturerInfo("Google Fit",      "com.google.android.apps.fitness")
    }
}

private fun openManufacturerApp(context: Context, info: ManufacturerInfo) {
    try {
        if (info.playStoreId != null) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${info.playStoreId}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        if (info.packageName == "com.google.android.apps.fitness") {
            openHealthConnectSettings(context)
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(info.packageName)
        if (intent != null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${info.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    } catch (e: Exception) {
        Log.e("Onboarding", "Failed to open manufacturer app: ${e.message}")
    }
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private fun openHealthConnectSettings(context: Context) {
    try {
        context.startActivity(
            Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        openAppSettings(context)
    }
}

private fun canShowNotifDialog(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val activity = context as? ComponentActivity ?: return true
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.POST_NOTIFICATIONS
    )
}

private suspend fun hasHealthConnectData(context: Context): Boolean {
    return try {
        val client   = HealthConnectClient.getOrCreate(context)
        val end      = Instant.now()
        val start    = end.minus(30, ChronoUnit.DAYS)
        val response = client.readRecords(
            ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end))
        )
        response.records.isNotEmpty()
    } catch (e: Exception) {
        Log.e("Onboarding", "HC data check failed: ${e.message}")
        false
    }
}

private val fullHealthPermissions = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
)

private val detectionHealthPermissions = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class)
)

private suspend fun isHealthConnectGranted(context: Context, permissions: Set<String>): Boolean {
    return try {
        val client  = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        permissions.all { it in granted }
    } catch (e: Exception) { false }
}

// ─── MAIN SCREEN ────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onPermissionsGranted: () -> Unit = {},
    viewOnly: Boolean = false
) {
    val context     = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages  = if (viewOnly) 5 else 6

    var ctaLabel        by remember { mutableStateOf("GET STARTED") }
    var ctaEnabled      by remember { mutableStateOf(true) }
    var permPageTrigger by remember { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (index == currentPage) 24.dp else 6.dp, height = 6.dp)
                            .clip(if (index == currentPage) RoundedCornerShape(3.dp) else CircleShape)
                            .background(if (index == currentPage) OrangeAccent else Color.Black.copy(alpha = 0.1f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = currentPage,
                modifier    = Modifier.weight(1f),
                transitionSpec = {
                    val gap = 0.15f
                    if (targetState > initialState)
                        slideInHorizontally { (it * (1 + gap)).toInt() } togetherWith slideOutHorizontally { -(it * (1 + gap)).toInt() }
                    else
                        slideInHorizontally { -(it * (1 + gap)).toInt() } togetherWith slideOutHorizontally { (it * (1 + gap)).toInt() }
                },
                label = "onboarding"
            ) { page ->
                when (page) {
                    0 -> PageHook()
                    1 -> PageGoal()
                    2 -> PageSmartDetection()
                    3 -> PageHowItWorks()
                    4 -> PageNotification()
                    5 -> if (!viewOnly) PagePermissions(
                        trigger              = permPageTrigger,
                        onPermissionsGranted = onPermissionsGranted,
                        onComplete           = onComplete,
                        onCtaLabelChange     = { ctaLabel = it },
                        onCtaEnabledChange   = { ctaEnabled = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick        = { currentPage-- },
                        shape          = CircleShape,
                        border         = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.Black.copy(alpha = 0.1f))
                        ),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                        colors         = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black.copy(alpha = 0.4f))
                    ) {
                        Text("BACK", fontSize = 12.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    enabled        = ctaEnabled,
                    onClick        = {
                        if (viewOnly) { onComplete(); return@Button }
                        if (currentPage < totalPages - 1) currentPage++
                        else permPageTrigger++
                    },
                    colors         = ButtonDefaults.buttonColors(
                        containerColor         = RamsSurface,
                        disabledContainerColor = RamsSurface.copy(alpha = 0.5f)
                    ),
                    shape          = CircleShape,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    val label = when {
                        viewOnly                     -> "CLOSE"
                        currentPage < totalPages - 1 -> "NEXT"
                        else                         -> ctaLabel
                    }
                    Text(
                        text          = label,
                        color         = if (ctaEnabled) OrangeAccent else OrangeAccent.copy(alpha = 0.4f),
                        fontSize      = 13.sp,
                        fontFamily    = JetBrainsMono,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    )
                }
            }
        }
    }
}

// ─── PAGE 6: PERMISSIONS ─────────────────────────────────────────────

@Composable
private fun PagePermissions(
    trigger: Int,
    onPermissionsGranted: () -> Unit,
    onComplete: () -> Unit,
    onCtaLabelChange: (String) -> Unit,
    onCtaEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val mfr     = remember { detectManufacturer() }

    var notifState      by remember { mutableStateOf(PermRowState.IDLE) }
    var activityState   by remember { mutableStateOf(PermRowState.IDLE) }
    var healthState     by remember { mutableStateOf(PermRowState.IDLE) }
    var batteryState    by remember { mutableStateOf(PermRowState.IDLE) }
    var mfrState        by remember { mutableStateOf(PermRowState.IDLE) }
    var dataState       by remember { mutableStateOf(PermRowState.IDLE) }

    var lastTrigger     by remember { mutableIntStateOf(0) }
    var notifAttempted  by remember { mutableStateOf(false) }
    var showSkipWarningSheet by remember { mutableStateOf(false) }
    var healthLaunchTime by remember { mutableLongStateOf(0L) }

    // ── Launchers ────────────────────────────────────────────────────

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifAttempted = true
        notifState = if (granted) PermRowState.GRANTED else PermRowState.FAILED
    }

    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        activityState = if (granted) PermRowState.GRANTED else PermRowState.FAILED
    }

    val healthLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val ok = granted.isNotEmpty()
        if (ok) {
            healthState = PermRowState.GRANTED
            onPermissionsGranted()
        } else {
            val elapsed = System.currentTimeMillis() - healthLaunchTime
            if (elapsed < 500L) {
                Log.d("Onboarding", "HC dialog suppressed (${elapsed}ms) — opening HC settings")
                openHealthConnectSettings(context)
            } else {
                healthState = PermRowState.FAILED
            }
        }
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
        else true
        batteryState = if (ok) PermRowState.GRANTED else PermRowState.FAILED
    }

    // ── Individual actions ────────────────────────────────────────────

    val provideNotif: () -> Unit = {
        notifAttempted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (alreadyGranted) {
                notifState = PermRowState.GRANTED
            } else {
                val canShow    = canShowNotifDialog(context)
                val neverAsked = notifState == PermRowState.IDLE
                if (neverAsked || canShow) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openAppSettings(context)
                }
            }
        } else {
            notifState = PermRowState.GRANTED
        }
    }

    val provideActivity: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            activityState = PermRowState.GRANTED
        }
    }

    val provideHealth: () -> Unit = {
        healthLaunchTime = System.currentTimeMillis()
        healthLauncher.launch(fullHealthPermissions)
    }

    val provideBattery: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                batteryLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            } else {
                batteryState = PermRowState.GRANTED
            }
        } else {
            batteryState = PermRowState.GRANTED
        }
    }

    val provideMfr: () -> Unit = {
        mfrState = PermRowState.LOADING
        openManufacturerApp(context, mfr)
    }

    val retryData: () -> Unit = {
        scope.launch {
            dataState = PermRowState.LOADING
            delay(1500)
            val hasData = hasHealthConnectData(context)
            dataState = if (hasData) PermRowState.GRANTED else PermRowState.FAILED
            if (hasData) mfrState = PermRowState.GRANTED
        }
    }

    // ── Initial render check ──────────────────────────────────────────

    LaunchedEffect(Unit) {
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true
        if (notifOk) { notifState = PermRowState.GRANTED; notifAttempted = true }

        val activityOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        else true
        if (activityOk) activityState = PermRowState.GRANTED

        val batteryOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
        else true
        if (batteryOk) batteryState = PermRowState.GRANTED

        val hcOk = isHealthConnectGranted(context, detectionHealthPermissions)
        if (hcOk) healthState = PermRowState.GRANTED
        if (hcOk) {
            dataState = PermRowState.LOADING
            val hasData = hasHealthConnectData(context)
            if (hasData) { dataState = PermRowState.GRANTED; mfrState = PermRowState.GRANTED }
            else dataState = PermRowState.IDLE
        }
    }

    // ── ON_RESUME re-check ────────────────────────────────────────────

    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    else true
                    if (notifOk) { notifState = PermRowState.GRANTED; notifAttempted = true }

                    val activityOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                    else true
                    if (activityOk) activityState = PermRowState.GRANTED

                    val hcOk = isHealthConnectGranted(context, detectionHealthPermissions)
                    if (hcOk && healthState != PermRowState.GRANTED) {
                        healthState = PermRowState.GRANTED
                        onPermissionsGranted()
                    }

                    val batteryOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
                    else true
                    if (batteryOk) batteryState = PermRowState.GRANTED

                    if (mfrState == PermRowState.LOADING) {
                        dataState = PermRowState.LOADING
                        delay(1500)
                        val hasData = hasHealthConnectData(context)
                        if (hasData) { mfrState = PermRowState.GRANTED; dataState = PermRowState.GRANTED }
                        else { mfrState = PermRowState.FAILED; dataState = PermRowState.FAILED }
                    }

                    if (hcOk && dataState == PermRowState.IDLE) {
                        dataState = PermRowState.LOADING
                        val hasData = hasHealthConnectData(context)
                        if (hasData) { mfrState = PermRowState.GRANTED; dataState = PermRowState.GRANTED }
                        else { dataState = PermRowState.FAILED }
                    }
                }
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    val allGranted = notifState == PermRowState.GRANTED && activityState == PermRowState.GRANTED && healthState == PermRowState.GRANTED &&
            batteryState == PermRowState.GRANTED && dataState == PermRowState.GRANTED
    val mandatoryMissing = healthState != PermRowState.GRANTED || activityState != PermRowState.GRANTED ||
            mfrState != PermRowState.GRANTED || dataState != PermRowState.GRANTED

    LaunchedEffect(allGranted, notifAttempted) {
        when {
            allGranted     -> { onCtaLabelChange("LET'S GO");     onCtaEnabledChange(true) }
            notifAttempted -> { onCtaLabelChange("SKIP FOR NOW"); onCtaEnabledChange(true) }
            else           -> { onCtaLabelChange("GET STARTED");  onCtaEnabledChange(true) }
        }
    }

    LaunchedEffect(trigger) {
        if (trigger == 0 || trigger == lastTrigger) return@LaunchedEffect
        lastTrigger = trigger
        when {
            allGranted       -> { onComplete() }
            !notifAttempted  -> { provideNotif() }
            mandatoryMissing -> { showSkipWarningSheet = true }
            else             -> { onComplete() }
        }
    }

    // ── UI ───────────────────────────────────────────────────────────

    Column(
        modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Almost there", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VictorMono, textAlign = TextAlign.Center, color = Color.Black.copy(alpha = 0.8f))
        Spacer(Modifier.height(32.dp))
        Text("PERMISSIONS", textAlign = TextAlign.Center, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = OrangeAccent)
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Grant the permissions below so Layzbug can track your walks and keep you on schedule.",
            modifier = Modifier.padding(horizontal = 24.dp), textAlign = TextAlign.Center,
            fontFamily = VictorMono, fontSize = 16.sp, lineHeight = 24.sp, color = Color.Black.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(40.dp))

        PermissionRow(label = "Notifications",        state = notifState,   onProvide = if (notifState    == PermRowState.IDLE) provideNotif    else null, onRetry = if (notifState    == PermRowState.FAILED) provideNotif    else null)
        PermissionRow(label = "Physical Activity",    state = activityState,onProvide = if (activityState == PermRowState.IDLE) provideActivity else null, onRetry = if (activityState == PermRowState.FAILED) provideActivity else null)
        PermissionRow(label = "Health Connect",       state = healthState,  onProvide = if (healthState   == PermRowState.IDLE) provideHealth   else null, onRetry = if (healthState   == PermRowState.FAILED) provideHealth   else null)
        PermissionRow(label = "Battery unrestricted", state = batteryState, onProvide = if (batteryState  == PermRowState.IDLE) provideBattery  else null, onRetry = if (batteryState  == PermRowState.FAILED) provideBattery  else null)
        PermissionRow(label = mfr.label,              state = mfrState,     onProvide = if (mfrState      == PermRowState.IDLE) provideMfr      else null, onRetry = if (mfrState      == PermRowState.FAILED) provideMfr      else null)
        PermissionRow(label = "Fitness data detected",state = dataState,    onProvide = null,                                                              onRetry = if (dataState     == PermRowState.FAILED) retryData       else null)
    }

    if (showSkipWarningSheet) {
        SkipWarningSheet(
            mfrLabel             = mfr.label,
            onProvidePermissions = {
                showSkipWarningSheet = false
                scope.launch {
                    delay(300)
                    when {
                        activityState != PermRowState.GRANTED -> provideActivity()
                        healthState   != PermRowState.GRANTED -> provideHealth()
                        mfrState      != PermRowState.GRANTED -> provideMfr()
                        else                                  -> retryData()
                    }
                }
            },
            onSkip    = { showSkipWarningSheet = false; onComplete() },
            onDismiss = { showSkipWarningSheet = false }
        )
    }
}

// ─── SKIP WARNING SHEET ──────────────────────────────────────────────

@Composable
private fun SkipWarningSheet(
    mfrLabel: String,
    onProvidePermissions: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val ToastRed      = Color(0xFF8B1A1A)
    val bodyTextMuted = Color.Black.copy(alpha = 0.6f)
    val headlineColor = Color(0xFF151619)

    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onDismiss, lightBackground = true) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier.height(28.dp).background(ToastRed, CircleShape).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFFF6B6B)))
                Text("PERMISSIONS REQUIRED", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = VictorMono, letterSpacing = 1.1.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text("Walk tracking won't work", color = headlineColor, fontSize = 18.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = (-0.3).sp)
            Spacer(Modifier.height(20.dp))
            Text("Health Connect, Physical Activity sensors, and $mfrLabel are required for Layzbug to automatically track your walks.", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            Spacer(Modifier.height(20.dp))
            Text("Proceeding without these means your walks will not be tracked automatically. You can still manually mark days as walked.", color = bodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            Spacer(Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onSkip() }, shape = CircleShape,
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Black.copy(alpha = 0.1f))),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(0.28f)
                ) {
                    Text("SKIP", fontSize = 12.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1)
                }
                Button(
                    onClick = { onProvidePermissions() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoalGreen),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                    modifier = Modifier.weight(0.72f)
                ) {
                    Text("PROVIDE PERMISSIONS", color = Color.White, fontSize = 13.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ─── PERMISSION ROW ──────────────────────────────────────────────────

@Composable
private fun PermissionRow(
    label: String,
    state: PermRowState,
    onProvide: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    Row(
        modifier              = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            when (state) {
                PermRowState.GRANTED -> Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GreenAccent))
                PermRowState.FAILED  -> Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OrangeAccent))
                PermRowState.LOADING -> CircularProgressIndicator(modifier = Modifier.size(14.dp), color = OrangeAccent, strokeWidth = 1.5.dp)
                PermRowState.IDLE    -> Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.15f)))
            }
        }

        Text(text = label, fontSize = 16.sp, fontFamily = VictorMono, lineHeight = 24.sp, color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.weight(1f))

        Box(modifier = Modifier.width(84.dp).height(42.dp), contentAlignment = Alignment.Center) {
            when {
                onProvide != null && state == PermRowState.IDLE -> {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFF0F0F0)).clickable { onProvide() }, contentAlignment = Alignment.Center) {
                        Text("PROVIDE", fontSize = 10.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = Color(0xFF1A6E35), letterSpacing = 0.8.sp, textAlign = TextAlign.Center)
                    }
                }
                onRetry != null && state == PermRowState.FAILED -> {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(OrangeAccent.copy(alpha = 0.1f)).clickable { onRetry() }, contentAlignment = Alignment.Center) {
                        Text("RETRY", fontSize = 10.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = OrangeAccent, letterSpacing = 0.8.sp, textAlign = TextAlign.Center)
                    }
                }
                else -> Spacer(modifier = Modifier.size(width = 84.dp, height = 42.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 8.dp).background(Color.Black.copy(alpha = 0.05f)))
}

// ─── PAGE 1: THE HOOK ───────────────────────────────────────────────

@Composable
private fun PageHook() {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue  = -12f, targetValue = 12f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label         = "offset"
    )
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painter = painterResource(id = R.drawable.ic_layzbug), contentDescription = null, modifier = Modifier.size(140.dp).offset(y = floatingOffset.dp))
        Spacer(modifier = Modifier.height(64.dp))
        Text("Science says 30 minutes of intentional walking changes everything.\n\nLayzbug helps you prove it.", textAlign = TextAlign.Center, fontFamily = VictorMono, fontSize = 18.sp, lineHeight = 28.sp, color = Color.Black.copy(alpha = 0.7f))
    }
}

// ─── PAGE 2: THE GOAL ───────────────────────────────────────────────

@Composable
private fun PageGoal() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Your Daily Objective", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = VictorMono, color = Color.Black.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier.size(280.dp).clip(CircleShape).background(RamsSurface).border(1.dp, RamsBorder, CircleShape).drawBehind {
                val g = 4.dp.toPx()
                for (x in 0..size.width.toInt() step g.toInt()) drawLine(RamsGridLine, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.dp.toPx())
                for (y in 0..size.height.toInt() step g.toInt()) drawLine(RamsGridLine, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
            },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("30", color = OrangeAccent, fontSize = 100.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, letterSpacing = (-4).sp,
                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(color = OrangeAccent.copy(alpha = 0.5f), offset = Offset.Zero, blurRadius = 50f)))
                Text("MINUTES OF", color = Color.White, fontSize = 14.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                Text("WALKING",    color = Color.White, fontSize = 14.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text("🙅🏼🐂💩", fontSize = 28.sp)
    }
}

// ─── PAGE 3: SMART DETECTION ────────────────────────────────────────

@Composable
private fun PageSmartDetection() {
    data class RainChip(val text: String, val isValid: Boolean, val column: Int, val duration: Int, val delay: Int)
    val chips = remember {
        listOf(
            RainChip("5 MINS",  true,  0, 4000, 0),    RainChip("3 MINS",  false, 1, 4500, 600),
            RainChip("8 MINS",  true,  2, 3800, 300),   RainChip("2 MINS",  false, 3, 4200, 900),
            RainChip("12 MINS", true,  1, 4300, 1500),  RainChip("4 MINS",  false, 0, 3900, 1200),
            RainChip("6 MINS",  true,  3, 4400, 2000),  RainChip("1 MIN",   false, 2, 4100, 1800),
            RainChip("15 MINS", true,  0, 4200, 2500),  RainChip("7 MINS",  true,  2, 4000, 2800),
            RainChip("4 MINS",  false, 1, 4300, 3200),  RainChip("10 MINS", true,  3, 3900, 3500),
        )
    }
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Not all minutes count", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = VictorMono, color = Color.Black.copy(0.8f))
        Spacer(modifier = Modifier.height(32.dp))
        Text("WALKS UNDER 5 MINUTES ARE FILTERED OUT", textAlign = TextAlign.Center, fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = OrangeAccent)
        Spacer(modifier = Modifier.height(40.dp))
        Box(modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f).clip(CircleShape).background(RamsSurface).border(1.dp, RamsBorder, CircleShape).drawBehind {
            val g = 4.dp.toPx()
            for (x in 0..size.width.toInt() step g.toInt()) drawLine(RamsGridLine, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.dp.toPx())
            for (y in 0..size.height.toInt() step g.toInt()) drawLine(RamsGridLine, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
        }) {
            chips.forEach { chip ->
                val fallY by infiniteTransition.animateFloat(initialValue = -100f, targetValue = 460f, animationSpec = infiniteRepeatable(animation = tween(chip.duration, chip.delay, LinearEasing), repeatMode = RepeatMode.Restart), label = "fall")
                val xFraction    = when (chip.column) { 0 -> 0.15f; 1 -> 0.40f; 2 -> 0.65f; 3 -> 0.85f; else -> 0.5f }
                val killProgress = if (!chip.isValid && fallY > 100f) ((fallY - 100f) / 100f).coerceIn(0f, 1f) else 0f
                val isExploding  = !chip.isValid && killProgress in 0.01f..0.99f
                val isDead       = !chip.isValid && killProgress >= 0.99f
                val chipAlpha    = when { chip.isValid -> 1f; killProgress < 0.1f -> 1f; else -> 0f }
                if (!isDead) {
                    Box(modifier = Modifier.fillMaxWidth().offset(y = fallY.dp)) {
                        Box(modifier = Modifier.align(BiasAlignment(xFraction * 2 - 1, 0f)).graphicsLayer { alpha = chipAlpha }) { RainChipPill(chip.text, chip.isValid) }
                    }
                }
                if (isExploding) ExplosionParticles("${chip.text}_${chip.column}_${chip.delay}", xFraction, fallY, killProgress)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text("Only intentional walks count.\nShorter walks to bathroom,\nkitchen, and in between rooms,\nare filtered out.", textAlign = TextAlign.Center, fontFamily = VictorMono, fontSize = 16.sp, lineHeight = 24.sp, color = Color.Black.copy(0.6f))
    }
}

@Composable
private fun RainChipPill(text: String, isValid: Boolean) {
    val c = if (isValid) GreenAccent else OrangeAccent
    Text(text, color = c, fontSize = 13.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(color = c.copy(alpha = 0.8f), offset = Offset.Zero, blurRadius = 0f)))
}

@Composable
private fun BoxScope.ExplosionParticles(chipKey: String, xFraction: Float, yOffset: Float, progress: Float) {
    val particles = remember(chipKey) {
        val r = kotlin.random.Random(chipKey.hashCode())
        List(50) { ParticleData(r.nextFloat() * 2f * Math.PI.toFloat(), 20f + r.nextFloat() * 60f, 0.4f + r.nextFloat() * 1.1f, 30f + r.nextFloat() * 40f, 0.7f + r.nextFloat() * 0.3f, r.nextFloat()) }
    }
    Box(modifier = Modifier.fillMaxWidth().offset(y = (yOffset - 90f).dp)) {
        Box(modifier = Modifier.align(BiasAlignment(xFraction * 2 - 1, 0f))) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val cx = size.width / 2; val cy = size.height / 2
                particles.forEach { p ->
                    val t = progress
                    val px = cx + kotlin.math.cos(p.angle) * p.speed * t * density
                    val py = cy + kotlin.math.sin(p.angle) * p.speed * t * density + (p.gravity * t * t * density)
                    val alpha = (1f - (t / p.lifeDecay).coerceAtMost(1f)).coerceAtLeast(0f)
                    if (alpha <= 0f) return@forEach
                    drawCircle(androidx.compose.ui.graphics.lerp(OrangeAccent, Color(0xFFFFAA00), p.colorMix).copy(alpha = alpha), (p.size * (1f - t * 0.3f)) * density.coerceAtLeast(0.5f), Offset(px, py))
                }
            }
        }
    }
}

private data class ParticleData(val angle: Float, val speed: Float, val size: Float, val gravity: Float, val lifeDecay: Float, val colorMix: Float)

// ─── PAGE 4: HOW IT WORKS ───────────────────────────────────────────

@Composable
private fun PageHowItWorks() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Zero manual tracking", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = VictorMono, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Text("WE SYNC WITH GOOGLE FIT\nIN THE BACKGROUND", textAlign = TextAlign.Center, fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = OrangeAccent)
        Spacer(modifier = Modifier.height(40.dp))
        OrbitingOrb(modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f))
        Spacer(modifier = Modifier.height(40.dp))
        Text("Your only job is to move. We handle the math and the calendar.", modifier = Modifier.padding(horizontal = 24.dp), textAlign = TextAlign.Center, fontFamily = VictorMono, fontSize = 16.sp, lineHeight = 24.sp, color = Color.Black.copy(0.6f))
    }
}

private data class OrbitParticle(val latitude: Float, val longitude: Float, val speed: Float, val size: Float, val colorMix: Float)

@Composable
private fun OrbitingOrb(modifier: Modifier = Modifier) {
    val particles = remember {
        val r = kotlin.random.Random(42)
        List(180) { OrbitParticle((r.nextFloat() - 0.5f) * 160f, r.nextFloat() * 360f, 0.6f + r.nextFloat() * 0.8f, 0.8f + r.nextFloat() * 1.4f, r.nextFloat()) }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val globalRotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), "rotation")
    Box(modifier = modifier.clip(CircleShape).background(RamsSurface).border(1.dp, RamsBorder, CircleShape), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2; val cy = size.height / 2; val r = size.minDimension / 2 * 0.92f
            particles.forEach { p ->
                val lonRad = Math.toRadians((p.longitude + globalRotation * p.speed).toDouble())
                val latRad = Math.toRadians(p.latitude.toDouble())
                val x = (kotlin.math.cos(latRad) * kotlin.math.sin(lonRad)).toFloat()
                val y = (kotlin.math.sin(latRad)).toFloat()
                val z = (kotlin.math.cos(latRad) * kotlin.math.cos(lonRad)).toFloat()
                val d = (z + 1f) / 2f
                drawCircle(androidx.compose.ui.graphics.lerp(OrangeAccent, Color(0xFFFFAA00), p.colorMix).copy(alpha = 0.3f + d * 0.7f), (p.size * (0.5f + d * 0.5f)) * density, Offset(cx + x * r, cy + y * r))
            }
        }
        Text("AUTO", color = Color.White.copy(0.0f), fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 2.sp)
    }
}

// ─── PAGE 5: NOTIFICATION ───────────────────────────────────────────

@Composable
private fun PageNotification() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Silence is earned", modifier = Modifier.padding(horizontal = 24.dp), color = Color.Black.copy(alpha = 0.8f), fontSize = 20.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Text("ZERO SPAM", textAlign = TextAlign.Center, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = OrangeAccent)
        Spacer(modifier = Modifier.height(32.dp))
        Text("You only get one notification a day, and only if you haven't walked for the day.", modifier = Modifier.padding(horizontal = 24.dp), textAlign = TextAlign.Center, fontFamily = VictorMono, fontSize = 16.sp, lineHeight = 24.sp, color = Color.Black.copy(0.6f))
        Spacer(modifier = Modifier.height(40.dp))
        RamsCard {
            Column(modifier = Modifier.padding(20.dp)) {
                ChipLabel("NOTIFICATION", OrangeAccent)
                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OrangeAccent))
                            Text("LAYZBUG", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("18:30", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontFamily = JetBrainsMono)
                        }
                        Text("No walk detected today.\nGet your ass moving! 🍑", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontFamily = VictorMono, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

// ─── SHARED COMPONENTS ──────────────────────────────────────────────

@Composable
private fun RamsCard(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(RamsSurface).border(1.dp, RamsBorder, RoundedCornerShape(24.dp)).drawBehind {
        val g = 4.dp.toPx()
        for (x in 0..size.width.toInt() step g.toInt()) drawLine(RamsGridLine, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.dp.toPx())
        for (y in 0..size.height.toInt() step g.toInt()) drawLine(RamsGridLine, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
    }) { content() }
}

@Composable
private fun ChipLabel(text: String, color: Color) {
    Row(modifier = Modifier.height(32.dp).background(RamsChipBg, CircleShape).border(1.dp, RamsBorder, CircleShape).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}