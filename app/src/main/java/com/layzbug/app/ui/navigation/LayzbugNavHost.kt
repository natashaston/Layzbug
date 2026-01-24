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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.layzbug.app.ui.screens.home.HomeScreen
import com.layzbug.app.ui.screens.HistoryScreen
import com.layzbug.app.ui.screens.MonthDetailScreen
import com.layzbug.app.ui.screens.PermissionScreen
import com.layzbug.app.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayzbugNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Initialize HealthConnectClient for the Permission Screen
    val healthConnectClient = HealthConnectClient.getOrCreate(context)

    Scaffold(
        containerColor = Surface,
        topBar = {
            // Only show TopBar if we are NOT on the permission screen
            if (currentRoute != Routes.Permission.route) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                "details" -> "January"
                                "history" -> "History"
                                else -> "Layzbug"
                            },
                            style = MaterialTheme.typography.headlineMedium
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
                        containerColor = Surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // Changed startDestination to "permission" to trigger the flow
            startDestination = Routes.Permission.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // New Permission Screen Route
            composable(Routes.Permission.route) {
                PermissionScreen(
                    navController = navController,
                    healthConnectClient = healthConnectClient
                )
            }

            composable("home") {
                HomeScreen(
                    onNavigateToHistory = { navController.navigate("history") },
                    onNavigateToMonthDetail = { navController.navigate("details") }
                )
            }

            composable("history") {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToMonth = { _, _ ->
                        navController.navigate("details")
                    }
                )
            }

            composable("details") {
                MonthDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}