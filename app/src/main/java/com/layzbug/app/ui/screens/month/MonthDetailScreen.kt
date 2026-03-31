package com.layzbug.app.ui.screens.month

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.layzbug.app.R
import com.layzbug.app.ui.components.CalendarGrid
import com.layzbug.app.ui.components.EditWalkStatusBottomSheet
import com.layzbug.app.ui.components.MonthHero
import com.layzbug.app.ui.theme.Dimens
import com.layzbug.app.ui.theme.SurfaceColor
import com.layzbug.app.data.viewmodel.MonthViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDetailScreen(
    onBack: () -> Unit,
    year: Int = YearMonth.now().year,
    month: Int = YearMonth.now().monthValue,
    viewModel: MonthViewModel = hiltViewModel()
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val currentMonth = remember(year, month) { YearMonth.of(year, month) }

    // Load month data as soon as params change
    LaunchedEffect(year, month) {
        viewModel.loadMonthData(YearMonth.of(year, month))
    }

    val walkDays by viewModel.walkDays.collectAsState()
    val rawMonthStats by viewModel.monthStats.collectAsState()
    val showSignInPrompt by viewModel.showSignInPrompt.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MonthDetailScreen", "📱 Sign-in result received: ${result.resultCode}")

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                Log.d("MonthDetailScreen", "✅ RESULT_OK")
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d("MonthDetailScreen", "✅ Got account: ${account.email}")

                    scope.launch {
                        try {
                            val success = viewModel.signInWithGoogle(account)
                            if (success) {
                                Log.d("MonthDetailScreen", "✅ Sign-in successful!")
                                snackbarHostState.showSnackbar("Signed in as ${account.email}")
                                viewModel.syncAfterSignIn()
                            } else {
                                Log.e("MonthDetailScreen", "❌ Sign-in failed")
                                snackbarHostState.showSnackbar("Sign in failed")
                            }
                        } catch (e: Exception) {
                            Log.e("MonthDetailScreen", "❌ Exception: ${e.message}", e)
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                } catch (e: ApiException) {
                    Log.e("MonthDetailScreen", "❌ ApiException: ${e.statusCode}", e)
                    scope.launch {
                        snackbarHostState.showSnackbar("Sign in error: ${e.statusCode}")
                    }
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d("MonthDetailScreen", "⚠️ Sign-in cancelled")
            }
            else -> {
                Log.e("MonthDetailScreen", "❌ Unknown result: ${result.resultCode}")
            }
        }
    }

    LaunchedEffect(showSignInPrompt) {
        if (showSignInPrompt) {
            val result = snackbarHostState.showSnackbar(
                message = "Sign in to sync walks across devices",
                actionLabel = "Sign In",
                duration = SnackbarDuration.Long
            )

            if (result == SnackbarResult.ActionPerformed) {
                viewModel.launchSignIn(signInLauncher)
            }

            viewModel.dismissSignInPrompt()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = paddingValues.calculateBottomPadding()
                )
        ) {
            MonthHero(
                stats = rawMonthStats,
                isCurrentMonth = year == YearMonth.now().year && month == YearMonth.now().monthValue,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.spaceBase))

            // Section divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 32.dp)
                    .alpha(0.3f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Black.copy(alpha = 0.1f)))
                Text(
                    text = "DAILY BREAKDOWN",
                    fontSize = 11.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = Color.Black
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Black.copy(alpha = 0.1f)))
            }

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
                showEditSheet = false
            },
            onNotWalked = {
                selectedDate?.let {
                    viewModel.setWalkStatus(it, false)
                }
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false }
        )
    }
}