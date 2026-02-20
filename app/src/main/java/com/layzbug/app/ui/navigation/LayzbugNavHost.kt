package com.layzbug.app.ui.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.layzbug.app.ui.screens.home.HomeScreen
import com.layzbug.app.ui.screens.home.HomeViewModel
import com.layzbug.app.ui.screens.history.HistoryScreen
import com.layzbug.app.ui.screens.month.MonthDetailScreen
import com.layzbug.app.ui.screens.PermissionScreen
import com.layzbug.app.ui.screens.SplashScreen
import com.layzbug.app.ui.theme.SurfaceColor
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

    val healthConnectClient = HealthConnectClient.getOrCreate(context)
    val homeViewModel: HomeViewModel = hiltViewModel()

    val showTopBar = currentRoute != "splash" && currentRoute != Routes.Permission.route
    val topBarAlpha by animateFloatAsState(
        targetValue = if (showTopBar) 1f else 0f,
        animationSpec = tween(400, easing = LinearEasing),
        label = "topBarAlpha"
    )

    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                currentRoute?.startsWith("details") == true -> {
                                    val year = navBackStackEntry?.arguments?.getString("year")?.toIntOrNull() ?: YearMonth.now().year
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
                        if (currentRoute != "home" && currentRoute != null) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
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
            composable("splash") {
                SplashScreen(
                    viewModel = homeViewModel,
                    onNavigateToPermissions = {
                        navController.navigate(Routes.Permission.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                    onSyncComplete = {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.Permission.route) {
                PermissionScreen(
                    navController = navController,
                    healthConnectClient = healthConnectClient
                )
            }

            composable("home") {
                HomeScreen(
                    onNavigateToHistory = {
                        if (navController.currentDestination?.route != "history") {
                            navController.navigate("history")
                        }
                    },
                    onNavigateToMonthDetail = {
                        // Check if already on month detail screen (any month)
                        if (navController.currentDestination?.route != "details/{year}/{month}") {
                            val now = LocalDate.now()
                            navController.navigate("details/${now.year}/${now.monthValue}")
                        }
                    }
                )
            }

            composable("history") {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToMonth = { year: Int, month: Int ->
                        navController.navigate("details/$year/$month")
                    }
                )
            }

            composable(route = "details/{year}/{month}") { backStackEntry ->
                val year = backStackEntry.arguments?.getString("year")?.toIntOrNull() ?: YearMonth.now().year
                val month = backStackEntry.arguments?.getString("month")?.toIntOrNull() ?: YearMonth.now().monthValue

                val monthViewModel: com.layzbug.app.data.viewmodel.MonthViewModel = hiltViewModel()

                // Load month data BEFORE MonthDetailScreen composes
                LaunchedEffect(year, month) {
                    monthViewModel.loadMonthData(YearMonth.of(year, month))
                }

                MonthDetailScreen(
                    onBack = { navController.popBackStack() },
                    year = year,
                    month = month,
                    viewModel = monthViewModel
                )
            }
        }
    }
}