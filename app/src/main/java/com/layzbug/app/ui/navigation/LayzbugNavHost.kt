package com.layzbug.app.ui.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.layzbug.app.data.InstallationTracker
import com.layzbug.app.ui.screens.home.HomeScreen
import com.layzbug.app.ui.screens.home.HomeViewModel
import com.layzbug.app.ui.screens.history.HistoryScreen
import com.layzbug.app.ui.screens.month.MonthDetailScreen
import com.layzbug.app.ui.screens.OnboardingScreen
import com.layzbug.app.ui.screens.SplashScreen
import com.layzbug.app.ui.theme.SurfaceColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayzbugNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val homeViewModel: HomeViewModel = hiltViewModel()

    // isLoggedIn is now a StateFlow in HomeViewModel — observed here so any
    // screen that calls homeViewModel.onUserSignedIn() updates the card instantly
    val isLoggedIn by homeViewModel.isLoggedIn.collectAsState()

    val installationTracker = remember { InstallationTracker(context) }
    val authManager = remember { com.layzbug.app.data.auth.AuthManager(context) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showTopBar = currentRoute != "splash" && currentRoute != "onboarding"
    val topBarAlpha by animateFloatAsState(
        targetValue = if (showTopBar) 1f else 0f,
        animationSpec = tween(400, easing = LinearEasing),
        label = "topBarAlpha"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Layzbug",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoggedIn) {
                    NavigationDrawerItem(
                        label = { Text("Logout") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                authManager.signOut()
                                homeViewModel.onUserSignedOut()
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = SurfaceColor,
            topBar = {
                if (showTopBar) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when {
                                    currentRoute?.startsWith("details") == true -> {
                                        val year  = navBackStackEntry?.arguments?.getString("year")?.toIntOrNull()  ?: YearMonth.now().year
                                        val month = navBackStackEntry?.arguments?.getString("month")?.toIntOrNull() ?: YearMonth.now().monthValue
                                        YearMonth.of(year, month).month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                                    }
                                    currentRoute == "history" -> "History"
                                    else -> "Layzbug"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        navigationIcon = {
                            if (currentRoute == "home" && isLoggedIn) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            } else if (currentRoute != "home" && currentRoute != null) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SurfaceColor,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.graphicsLayer { alpha = topBarAlpha }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(route = "splash", exitTransition = { ExitTransition.None }) {
                    SplashScreen(
                        viewModel = homeViewModel,
                        onNavigateToOnboarding = {
                            navController.navigate("onboarding") { popUpTo("splash") { inclusive = true } }
                        },
                        onSyncComplete = {
                            navController.navigate("home") { popUpTo("splash") { inclusive = true } }
                        }
                    )
                }

                composable(
                    route = "onboarding",
                    enterTransition = { fadeIn(animationSpec = tween(500)) },
                    exitTransition = { ExitTransition.None }
                ) {
                    OnboardingScreen(
                        onComplete = {
                            installationTracker.setOnboardingComplete()
                            navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                        }
                    )
                }

                composable(
                    route = "home",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None }
                ) {
                    HomeScreen(
                        onNavigateToHistory = {
                            if (navController.currentDestination?.route != "history")
                                navController.navigate("history")
                        },
                        onNavigateToMonthDetail = {
                            if (navController.currentDestination?.route != "details/{year}/{month}") {
                                val now = LocalDate.now()
                                navController.navigate("details/${now.year}/${now.monthValue}")
                            }
                        },
                        isLoggedIn = isLoggedIn,
                        onSignInSuccess = { homeViewModel.onUserSignedIn() }
                    )
                }

                composable(
                    route = "history",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    HistoryScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToMonth = { year: Int, month: Int ->
                            navController.navigate("details/$year/$month")
                        }
                    )
                }

                composable(
                    route = "details/{year}/{month}",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) { backStackEntry ->
                    val year  = backStackEntry.arguments?.getString("year")?.toIntOrNull()  ?: YearMonth.now().year
                    val month = backStackEntry.arguments?.getString("month")?.toIntOrNull() ?: YearMonth.now().monthValue
                    val monthViewModel: com.layzbug.app.data.viewmodel.MonthViewModel = hiltViewModel()

                    LaunchedEffect(year, month) {
                        monthViewModel.loadMonthData(YearMonth.of(year, month))
                    }

                    MonthDetailScreen(
                        onBack = { navController.popBackStack() },
                        year = year,
                        month = month,
                        viewModel = monthViewModel,
                        // Pass homeViewModel.onUserSignedIn so the card disappears instantly
                        onSignInSuccess = { homeViewModel.onUserSignedIn() }
                    )
                }
            }
        }
    }
}