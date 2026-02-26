package com.layzbug.app.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.navigation.NavController
import com.layzbug.app.R
import com.layzbug.app.ui.navigation.Routes
import com.layzbug.app.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PermissionScreen(
    navController: NavController,
    healthConnectClient: HealthConnectClient
) {
    val coroutineScope = rememberCoroutineScope()

    // Vertical offset animation
    val logoOffset = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(1f) }
    val contentAlpha = remember { Animatable(0f) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
    )

    // Animate logo sliding up after initial delay
    LaunchedEffect(Unit) {
        delay(500)
        launch {
            logoOffset.animateTo(
                targetValue = -120f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            delay(300)
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        Log.d("PermissionScreen", "Granted permissions: $grantedPermissions")
        Log.d("PermissionScreen", "Navigating to home")
        navController.navigate("home") {
            popUpTo(Routes.Permission.route) { inclusive = true }
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
            // Logo and title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = logoOffset.value.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                    }
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
            }

            // Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 240.dp)
                    .graphicsLayer {
                        alpha = contentAlpha.value
                    }
            ) {
                Text(
                    text = "Track your 30+ minute walks automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Dimens.spaceXl)
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            requestPermissionLauncher.launch(permissions)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(Dimens.radius2xl)
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}