package com.layzbug.app.ui.screens

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.layzbug.app.R
import com.layzbug.app.ui.screens.home.HomeViewModel
import com.layzbug.app.ui.theme.Dimens
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: HomeViewModel,
    onNavigateToPermissions: () -> Unit,
    onSyncComplete: () -> Unit
) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        Log.d("SplashScreen", "🚀 Starting splash screen")

        // Fade in animation
        alpha.animateTo(1f, animationSpec = tween(500))

        val startTime = System.currentTimeMillis()

        // Check permissions
        val hasPerms = viewModel.checkPermissions()
        Log.d("SplashScreen", "Permissions: $hasPerms")

        if (hasPerms) {
            // Start sync and wait for it
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
        }

        // Enforce minimum 1.5s brand presence
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < 1500) {
            delay(1500 - elapsed)
        }

        // Navigate
        Log.d("SplashScreen", "Navigating... hasPerms=$hasPerms")
        if (hasPerms) {
            onSyncComplete()
        } else {
            onNavigateToPermissions()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.onPrimary
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(alpha.value)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.layzbug_img),
                    contentDescription = "Layzbug Logo",
                    modifier = Modifier.size(180.dp)
                )

                Spacer(modifier = Modifier.height(Dimens.spaceBase))

                Text(
                    text = "Hey Layzbug",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // No syncing indicator - keeps layout stable
            }
        }
    }
}