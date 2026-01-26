package com.layzbug.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.layzbug.app.ui.screens.home.HomeScreen
import com.layzbug.app.ui.screens.home.HomeViewModel
import com.layzbug.app.ui.screens.HistoryScreen
import com.layzbug.app.ui.screens.MonthDetailScreen
import com.layzbug.app.ui.screens.PermissionScreen
import com.layzbug.app.ui.screens.SplashScreen
import com.layzbug.app.ui.theme.SurfaceColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayzbugNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // HealthConnectClient instance
    val healthConnectClient = HealthConnectClient.getOrCreate(context)

    // Injecting the ViewModel at the NavHost level so it can be shared
    val homeViewModel: HomeViewModel = hiltViewModel()

    Scaffold(
        containerColor = SurfaceColor,
        topBar = {
            // Hide TopBar on Splash and Permission screens for a cleaner look
            val isEntryScreen = currentRoute == "splash" || currentRoute == Routes.Permission.route

            if (!isEntryScreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                "details" -> "January"
                                "history" -> "History"
                                else -> "Layzbug"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        // Show back button only if we aren't on Home
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
                    )
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash", // App always starts here to check sync/perms
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Splash Logic
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

            // 2. Permission Screen (The FAB screen)
            composable(Routes.Permission.route) {
                PermissionScreen(
                    navController = navController,
                    healthConnectClient = healthConnectClient
                )
            }

            // 3. Home Screen
            composable("home") {
                HomeScreen(
                    onNavigateToHistory = { navController.navigate("history") },
                    onNavigateToMonthDetail = { navController.navigate("details") }
                )
            }

            // 4. History Screen
            composable("history") {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToMonth = { _, _ ->
                        navController.navigate("details")
                    }
                )
            }

            // 5. Month Details
            composable("details") {
                MonthDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}