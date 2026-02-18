package com.layzbug.app.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.layzbug.app.ui.components.CalendarGrid
import com.layzbug.app.ui.components.EditWalkStatusBottomSheet
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceColor
import com.layzbug.app.data.viewmodel.MonthViewModel
import com.layzbug.app.ui.components.MonthHeroPill
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDetailScreen(
    onBack: () -> Unit,
    year: Int = YearMonth.now().year,
    month: Int = YearMonth.now().monthValue,
    viewModel: MonthViewModel = hiltViewModel()
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    val currentMonth = remember(year, month) { YearMonth.of(year, month) }
    val walkDays by viewModel.walkDays.collectAsState()
    val rawMonthStats by viewModel.monthStats.collectAsState()
    val showSignInPrompt by viewModel.showSignInPrompt.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                scope.launch {
                    viewModel.signInWithGoogle(account)
                }
            } catch (e: ApiException) {
                scope.launch {
                    snackbarHostState.showSnackbar("Sign in cancelled")
                }
            }
        }
    }

    // Show sign-in snackbar when triggered
    LaunchedEffect(showSignInPrompt) {
        Log.d("MonthDetailScreen", "showSignInPrompt changed to: $showSignInPrompt")
        if (showSignInPrompt) {
            Log.d("MonthDetailScreen", "Showing snackbar...")
            val result = snackbarHostState.showSnackbar(
                message = "Sign in to sync walks across devices",
                actionLabel = "Sign In",
                duration = SnackbarDuration.Long
            )
            Log.d("MonthDetailScreen", "Snackbar result: $result")
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.launchSignIn(signInLauncher)
            }
            viewModel.dismissSignInPrompt()
        }
    }

    LaunchedEffect(currentMonth) {
        viewModel.loadMonthData(currentMonth)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            MonthHeroPill(
                stats = rawMonthStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(238.dp)
            )

            Spacer(modifier = Modifier.height(Dimens.spaceLg))

            CalendarGrid(
                days = walkDays,
                onDayClick = { clickedDay ->
                    selectedDate = clickedDay.date
                    showEditSheet = true
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showEditSheet && selectedDate != null) {
        val walkDay = walkDays.find { it.date == selectedDate }
        EditWalkStatusBottomSheet(
            isVisible = showEditSheet,
            dateLabel = "${selectedDate?.month?.name?.lowercase()?.replaceFirstChar { it.uppercase() }} ${selectedDate?.dayOfMonth}",
            currentStatus = walkDay?.walked ?: false,
            onWalked = {
                selectedDate?.let {
                    viewModel.setWalkStatus(it, true)
                }
                scope.launch {
                    kotlinx.coroutines.delay(300)
                    showEditSheet = false
                }
            },
            onNotWalked = {
                selectedDate?.let {
                    viewModel.setWalkStatus(it, false)
                }
                scope.launch {
                    kotlinx.coroutines.delay(300)
                    showEditSheet = false
                }
            },
            onDismiss = { showEditSheet = false }
        )
    }
}