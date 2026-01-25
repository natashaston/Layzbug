package com.layzbug.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // Vital for the 'by' delegate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.layzbug.app.ui.screens.home.HomeViewModel

@Composable
fun SplashScreen(
    viewModel: HomeViewModel,
    onNavigateToPermissions: () -> Unit,
    onSyncComplete: () -> Unit // Match the name used in your NavHost
) {
    // Collect the StateFlow from the ViewModel
    val isSyncing by viewModel.isSyncing.collectAsState()

    // 1. Initial Check: Run once on launch
    LaunchedEffect(Unit) {
        val hasPerms = viewModel.checkPermissions()
        if (!hasPerms) {
            onNavigateToPermissions()
        }
    }

    // 2. Navigation Trigger: Only navigate to Home if sync finishes AND permissions exist
    LaunchedEffect(isSyncing) {
        // We use checkPermissions() here because that's the name in your ViewModel
        if (!isSyncing && viewModel.checkPermissions()) {
            onSyncComplete()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Layzbug",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Syncing your walks...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}