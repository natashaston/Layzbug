package com.layzbug.app.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.layzbug.app.R
import com.layzbug.app.data.viewmodel.MonthViewModel
import com.layzbug.app.ui.navigation.Routes
import com.layzbug.app.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PermissionScreen(
    navController: NavController,
    healthConnectClient: HealthConnectClient,
    viewModel: MonthViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf("signin") }

    // Vertical offset animation - starts at 0 (center), slides up to negative value
    val logoOffset = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(1f) } // Start at full opacity immediately
    val contentAlpha = remember { Animatable(0f) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        "android.permission.health.READ_HEALTH_DATA_HISTORY"
    )

    // Animate logo sliding up after initial delay
    LaunchedEffect(Unit) {
        delay(500) // Pause at splash position
        // Slide logo up and fade in content simultaneously
        launch {
            logoOffset.animateTo(
                targetValue = -120f, // Slide up by 120dp
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            delay(300) // Start content fade slightly after slide begins
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    coroutineScope.launch {
                        val success = viewModel.signInWithGoogle(account)
                        if (success) {
                            Log.d("PermissionScreen", "✅ Signed in, moving to permissions")
                            currentStep = "permissions"
                        }
                    }
                } catch (e: ApiException) {
                    Log.e("PermissionScreen", "Sign-in error: ${e.statusCode}")
                }
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        // Fade out logo before navigating
        coroutineScope.launch {
            logoAlpha.animateTo(0f, animationSpec = tween(100))
            navController.navigate("home") {
                popUpTo(Routes.Permission.route) { inclusive = true }
            }
        }
    }

    // Auto-request permissions when step changes
    LaunchedEffect(currentStep) {
        if (currentStep == "permissions") {
            requestPermissionLauncher.launch(permissions)
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
            // Logo and title - starts centered, slides up
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

            // Content - appears below logo after it slides up
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
                when (currentStep) {
                    "signin" -> {
                        Text(
                            text = "Sign in to sync your walks across devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = Dimens.spaceXl)
                        )

                        Button(
                            onClick = {
                                viewModel.launchSignIn(signInLauncher)
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
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    "permissions" -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Dimens.spaceBase))
                        Text(
                            text = "Requesting permissions...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}