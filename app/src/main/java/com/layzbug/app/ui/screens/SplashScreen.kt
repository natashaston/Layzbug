package com.layzbug.app.ui.screens

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.layzbug.app.R
import com.layzbug.app.data.InstallationTracker
import com.layzbug.app.ui.screens.home.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: HomeViewModel,
    installationTracker: InstallationTracker,
    onNavigateToOnboarding: () -> Unit,
    onSyncComplete: () -> Unit
) {
    val isSyncing   by viewModel.isSyncing.collectAsState()
    val alpha       = remember { Animatable(0f) }
    var syncStarted by remember { mutableStateOf(false) }

    // Watches isSyncing true → false after sync was kicked off here
    LaunchedEffect(isSyncing) {
        if (syncStarted && !isSyncing) {
            Log.d("SplashScreen", "✅ Sync complete — navigating home")
            delay(500)
            onSyncComplete()
        }
    }

    LaunchedEffect(Unit) {
        Log.d("SplashScreen", "🚀 Starting splash screen")
        alpha.animateTo(1f, animationSpec = tween(500))

        // ── DEBUG: log all prefs state on every launch ────────────────
        val onboardingDone  = installationTracker.isOnboardingComplete()
        val syncDone        = installationTracker.hasInitialSyncDone()
        val syncStartDate   = installationTracker.getSyncStartDate()
        val lastDailySync   = installationTracker.getLastDailySyncDate()
        Log.d("SplashDebug", "=== SPLASH STATE ===")
        Log.d("SplashDebug", "onboardingDone  = $onboardingDone")
        Log.d("SplashDebug", "syncDone        = $syncDone")
        Log.d("SplashDebug", "syncStartDate   = $syncStartDate")
        Log.d("SplashDebug", "lastDailySync   = $lastDailySync")
        Log.d("SplashDebug", "====================")

        Log.d("SplashScreen", "Onboarding complete: $onboardingDone")

        if (!onboardingDone) {
            // First time ever — always show onboarding.
            // No permission check, no shortcuts. Onboarding handles everything.
            delay(1000)
            Log.d("SplashScreen", "📋 Onboarding not done — showing onboarding")
            onNavigateToOnboarding()
            return@LaunchedEffect
        }

        // ── Returning user — onboarding already completed ─────────────
        // Go home. Permission and sync state is handled by HomeScreen's
        // ON_RESUME observer and HomeViewModel — not by splash.

        val wasAlreadySynced = viewModel.hasInitialSyncCompleted()
        Log.d("SplashDebug", "wasAlreadySynced = $wasAlreadySynced")

        if (wasAlreadySynced) {
            Log.d("SplashScreen", "✅ Already synced — navigating home directly")
            delay(800)
            onSyncComplete()
        } else {
            val hasPerms = viewModel.checkPermissions()
            Log.d("SplashDebug", "hasPerms = $hasPerms")
            if (hasPerms) {
                Log.d("SplashScreen", "✅ Perms granted — starting initial sync")
                syncStarted = true
                viewModel.startInitialSync()
            } else {
                Log.d("SplashScreen", "✅ No perms yet — navigating home, toast will show")
                delay(800)
                onSyncComplete()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text          = "LAYZBUG",
                fontFamily    = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal)),
                fontSize      = 28.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 8.sp,
                color         = Color(0xFF151619),
                modifier      = Modifier.alpha(alpha.value)
            )
        }
    }
}