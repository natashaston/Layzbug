package com.layzbug.app.ui.screens.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.layzbug.app.domain.StatsValue
import com.layzbug.app.ui.components.StatsCard
import com.layzbug.app.ui.components.StatsCardPill
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.OnPrimary
import com.layzbug.app.ui.theme.OnSurface
import com.layzbug.app.ui.theme.Primary
import com.layzbug.app.ui.theme.SurfaceContainer
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
    val weeklyDays by viewModel.weeklyDays.collectAsState()

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)
        ) {
            StatsCard(
                number = yearlyWalks.value.toString(),
                label = yearlyWalks.label,
                distanceKm = yearlyWalks.distanceKm,
                modifier = Modifier.weight(1f).graphicsLayer(),
                onClick = onNavigateToHistory
            )

            StatsCardPill(
                stats = currentMonthWalks,
                modifier = Modifier.weight(1f).graphicsLayer(),
                onClick = onNavigateToMonthDetail
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spaceXl3))
        Text(
            "Weekly Progress",
            color = OnSurface,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(Dimens.spaceXs2))
        Text(
            "30 minute walks",
            color = OnSurface,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(Dimens.spaceBase))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            items(weeklyDays) { day ->
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .clip(RoundedCornerShape(Dimens.radiusLg))
                        .background(if (day.isWalked) Primary else SurfaceContainer)
                        .clickable { viewModel.toggleDay(day.date, day.isWalked) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.label,
                        style = if (day.isWalked) {
                            MaterialTheme.typography.bodySmall.copy(color = OnPrimary)
                        } else {
                            MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }
        }
    }
}