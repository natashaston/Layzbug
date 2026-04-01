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
import com.layzbug.app.ui.screens.home.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: HomeViewModel,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onSyncComplete: () -> Unit,
    isOnboardingComplete: Boolean
) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        Log.d("SplashScreen", "🚀 Starting splash screen")

        val startTime = System.currentTimeMillis()

        // Check permissions FIRST
        val hasPerms = viewModel.checkPermissions()
        Log.d("SplashScreen", "Permissions: $hasPerms")

        if (hasPerms) {
            // Skip animation, go straight to home after sync
            Log.d("SplashScreen", "Starting sync...")
            viewModel.startInitialSync()

            // Wait for sync to complete (with 30 second timeout)
            var waitTime = 0L
            while (isSyncing && waitTime < 30000) {
                delay(100)
                waitTime += 100
            }

            Log.d("SplashScreen", "Sync complete after ${waitTime}ms")

            // Give time for database writes and UI refresh
            delay(500)

            // Navigate immediately without animation
            onSyncComplete()
        } else {
            // First time - show animation
            alpha.animateTo(1f, animationSpec = tween(500))

            // Enforce minimum 1.5s brand presence
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 1500) {
                delay(1500 - elapsed)
            }

            // Route: onboarding first if not completed, then permissions
            if (!isOnboardingComplete) {
                Log.d("SplashScreen", "📋 First install — showing onboarding")
                onNavigateToOnboarding()
            } else {
                Log.d("SplashScreen", "🔑 Onboarding done — showing permissions")
                onNavigateToPermissions()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "LAYZBUG",
                fontFamily = FontFamily(Font(R.font.jetbrains_mono_regular, FontWeight.Normal)),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                color = Color(0xFF151619),
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}