package com.layzbug.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme // Ensure this is here
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.layzbug.app.R
import com.layzbug.app.ui.screens.home.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import com.layzbug.app.ui.theme.OnSurfaceVariant

@Composable
fun SplashScreen(
    viewModel: HomeViewModel,
    onNavigateToPermissions: () -> Unit,
    onSyncComplete: () -> Unit
) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animation runs in parallel with the sync check
        alpha.animateTo(1f, animationSpec = tween(500))

        val startTime = System.currentTimeMillis()

        snapshotFlow { isSyncing }.collectLatest { syncing ->
            // Only proceed once syncing is false (Google Fit data fetched)
            if (!syncing) {
                val hasPerms = viewModel.checkPermissions()
                val elapsed = System.currentTimeMillis() - startTime

                // Enforce the 1.5s brand presence
                if (elapsed < 1500) delay(1500 - elapsed)

                if (hasPerms) onSyncComplete() else onNavigateToPermissions()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        // colorScheme.onPrimary is your 0xFFFFFFFF (White)
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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Hey Layzbug",
                    // headlineLarge = Google Sans Flex @ 500 weight
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Checking your progress...",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}