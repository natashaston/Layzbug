package com.layzbug.app.ui.screens.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.layzbug.app.ui.components.YearlyStatsWithDropdown
import com.layzbug.app.ui.components.MonthHero
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToMonthDetail: () -> Unit,
    isLoggedIn: Boolean = true,
    onSignInSuccess: () -> Unit = {}
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val yearlyWalks by viewModel.yearlyWalks.collectAsState()
    val currentMonthWalks by viewModel.currentMonthWalks.collectAsState()

    // Animate sign-in banner
    val bannerVisible = !isLoggedIn
    val bannerHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (bannerVisible) 140.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 300,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "bannerHeight"
    )

    // Google Sign-In launcher
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
                    }
                } catch (e: ApiException) {
                    // Handle error
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceColor)
            .padding(horizontal = Dimens.spaceBase)
    ) {
        // Sign-in prompt banner - animated
        androidx.compose.animation.AnimatedVisibility(
            visible = bannerVisible,
            enter = androidx.compose.animation.expandVertically(
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.spaceBase),
                shape = RoundedCornerShape(Dimens.radius5xl),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.spaceBase),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign in to sync manually marked walks across devices",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Dimens.spaceBase))

                    Button(
                        onClick = {
                            val authManager = com.layzbug.app.data.auth.AuthManager(context)
                            val signInIntent = authManager.getGoogleSignInClient().signInIntent
                            signInLauncher.launch(signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
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
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceBase)
        ) {
            YearlyStatsWithDropdown(
                totalWalks = yearlyWalks.value,
                totalDistanceKm = yearlyWalks.distanceKm,
                selectedYear = LocalDate.now().year,
                showDropdown = false,
                onClick = onNavigateToHistory,
                modifier = Modifier.fillMaxWidth()
            )

            MonthHero(
                stats = currentMonthWalks,
                modifier = Modifier.fillMaxWidth().graphicsLayer(),
                onClick = onNavigateToMonthDetail
            )
        }
    }
}