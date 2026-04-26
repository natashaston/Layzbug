package com.layzbug.app.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.layzbug.app.R
import com.layzbug.app.data.InstallationTracker
import com.layzbug.app.notifications.NotificationPrefsRepository
import com.layzbug.app.ui.screens.OnboardingScreen
import com.layzbug.app.ui.screens.SplashScreen
import com.layzbug.app.ui.screens.history.HistoryScreen
import com.layzbug.app.ui.screens.home.HomeScreen
import com.layzbug.app.ui.screens.home.HomeViewModel
import com.layzbug.app.ui.screens.month.MonthDetailScreen
import com.layzbug.app.ui.theme.SurfaceColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

// ─── FONTS ──────────────────────────────────────────────────────────

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

// ─── PALETTE ────────────────────────────────────────────────────────

private val RamsSurface   = Color(0xFF151619)
private val RamsBorder    = Color.White.copy(alpha = 0.05f)
private val RamsGridLine  = Color.Gray.copy(alpha = 0.03f)
private val RamsChipBg    = Color.White.copy(alpha = 0.03f)
private val OrangeAccent  = Color(0xFFFF4400)
private val GreenAccent   = Color(0xFF00FF66)
private val BodyTextMuted = Color.Black.copy(alpha = 0.6f)
private val HeadlineColor = Color(0xFF151619)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayzbugNavHost(
    notificationPrefs: NotificationPrefsRepository = hiltViewModel<LayzbugNavViewModel>().notificationPrefs
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val homeViewModel: HomeViewModel = hiltViewModel()
    val isLoggedIn by homeViewModel.isLoggedIn.collectAsState()

    val installationTracker = remember { InstallationTracker(context) }
    val authManager = remember { com.layzbug.app.data.auth.AuthManager(context) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Notification prefs
    val notifEnabled by notificationPrefs.isEnabled.collectAsState(initial = true)
    val notifHour    by notificationPrefs.notifHour.collectAsState(initial = 18)
    var showTimePicker   by remember { mutableStateOf(false) }
    var showHowItWorks   by remember { mutableStateOf(false) }
    var showSyncInfoSheet        by remember { mutableStateOf(false) }
    var showDrawerSignedInToast  by remember { mutableStateOf(false) }
    var showDrawerInfoSheet      by remember { mutableStateOf(false) }

    val currentUserEmail = remember(isLoggedIn) {
        if (isLoggedIn) GoogleSignIn.getLastSignedInAccount(context)?.email else null
    }

    // Sign-in launcher for the drawer CTA
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    scope.launch {
                        val am = com.layzbug.app.data.auth.AuthManager(context)
                        am.signInWithGoogle(account)
                        homeViewModel.onUserSignedIn()
                        showDrawerSignedInToast = true
                    }
                } catch (_: ApiException) {}
            }
        }
    }

    val showTopBar = currentRoute != "splash" && currentRoute != "onboarding"
    val topBarAlpha by animateFloatAsState(
        targetValue = if (showTopBar) 1f else 0f,
        animationSpec = tween(400, easing = LinearEasing),
        label = "topBarAlpha"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == "home",
        drawerContent = {
            // ── Full-width drawer ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                BackHandler(enabled = drawerState.isOpen) {
                    scope.launch { drawerState.close() }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Top bar row — back arrow + Settings title ────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── NOTIFICATIONS section ────────────────────────
                    SectionLabel("NOTIFICATIONS")
                    Spacer(modifier = Modifier.height(12.dp))

                    // showTimeRow mirrors notifEnabled but with a delay
                    // so toggle settles before time row appears/disappears
                    var showTimeRow by remember { mutableStateOf(notifEnabled) }
                    LaunchedEffect(notifEnabled) {
                        if (notifEnabled) {
                            kotlinx.coroutines.delay(260)
                            showTimeRow = true
                        } else {
                            showTimeRow = false
                        }
                    }

                    // Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily walk reminder",
                            fontSize = 15.sp,
                            fontFamily = VictorMono,
                            fontWeight = FontWeight.Medium,
                            color = BodyTextMuted
                        )
                        Switch(
                            checked = notifEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { notificationPrefs.setEnabled(enabled, notifHour) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor    = GreenAccent,
                                checkedTrackColor    = RamsSurface,
                                checkedBorderColor   = Color.Transparent,
                                uncheckedThumbColor  = Color(0xFF888888),
                                uncheckedTrackColor  = Color(0xFFE0E0E0),
                                uncheckedBorderColor = Color(0xFFBBBBBB)
                            )
                        )
                    }

                    // Time row — animated in/out after toggle settles
                    AnimatedVisibility(
                        visible = showTimeRow,
                        enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                        exit  = shrinkVertically(tween(200)) + fadeOut(tween(200))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reminder time",
                                    fontSize = 15.sp,
                                    fontFamily = VictorMono,
                                    fontWeight = FontWeight.Medium,
                                    color = BodyTextMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(RamsSurface)
                                        .border(1.dp, RamsBorder, CircleShape)
                                        .clickable { showTimePicker = true }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "%02d:00".format(notifHour),
                                        color = OrangeAccent,
                                        fontSize = 14.sp,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(24.dp))

                    // ── ABOUT section ────────────────────────────────
                    SectionLabel("ABOUT")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHowItWorks = true }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "How Layzbug works",
                            fontSize = 15.sp,
                            fontFamily = VictorMono,
                            fontWeight = FontWeight.Medium,
                            color = BodyTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(24.dp))

                    // ── ACCOUNT section ──────────────────────────────
                    SectionLabel("ACCOUNT")
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoggedIn) {
                        // Signed-in toast — shows after login from drawer
                        if (showDrawerSignedInToast) {
                            DrawerSignedInToast(
                                onDismiss = { showDrawerSignedInToast = false },
                                onInfo    = { showDrawerInfoSheet = true }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        // Email
                        if (currentUserEmail != null) {
                            Text(
                                text = currentUserEmail,
                                fontSize = 13.sp,
                                fontFamily = VictorMono,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        // Logout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        authManager.signOut()
                                        homeViewModel.onUserSignedOut()
                                        showDrawerSignedInToast = false
                                        drawerState.close()
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Logout",
                                fontSize = 15.sp,
                                fontFamily = VictorMono,
                                fontWeight = FontWeight.Medium,
                                color = OrangeAccent
                            )
                        }
                    } else {
                        // ── Cloud sync off card ──────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .background(RamsSurface)
                                .border(1.dp, RamsBorder, RoundedCornerShape(36.dp))
                                .clickable { showSyncInfoSheet = true }
                                .drawBehind {
                                    val gridSize = 4.dp.toPx()
                                    for (x in 0..size.width.toInt() step gridSize.toInt())
                                        drawLine(RamsGridLine, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.dp.toPx())
                                    for (y in 0..size.height.toInt() step gridSize.toInt())
                                        drawLine(RamsGridLine, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .background(RamsChipBg, CircleShape)
                                        .border(1.dp, RamsBorder, CircleShape)
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OrangeAccent))
                                    Text(
                                        text = "CLOUD SYNC IS OFF",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontFamily = VictorMono,
                                        letterSpacing = 1.1.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GoogleGLogoDrawer(modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
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
                            if (currentRoute == "home") {
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
                        onNavigateToOnboarding = { navController.navigate("onboarding") { popUpTo("splash") { inclusive = true } } },
                        onSyncComplete = { navController.navigate("home") { popUpTo("splash") { inclusive = true } } }
                    )
                }
                composable(route = "onboarding", enterTransition = { fadeIn(animationSpec = tween(500)) }, exitTransition = { ExitTransition.None }) {
                    OnboardingScreen(onComplete = {
                        installationTracker.setOnboardingComplete()
                        navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                    })
                }
                composable(route = "home", enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    HomeScreen(
                        onNavigateToHistory = { if (navController.currentDestination?.route != "history") navController.navigate("history") },
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
                composable(route = "history", enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }, popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }) {
                    HistoryScreen(onBack = { navController.popBackStack() }, onNavigateToMonth = { year, month -> navController.navigate("details/$year/$month") })
                }
                composable(route = "details/{year}/{month}", enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }, popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }) { backStackEntry ->
                    val year  = backStackEntry.arguments?.getString("year")?.toIntOrNull()  ?: YearMonth.now().year
                    val month = backStackEntry.arguments?.getString("month")?.toIntOrNull() ?: YearMonth.now().monthValue
                    val monthViewModel: com.layzbug.app.data.viewmodel.MonthViewModel = hiltViewModel()
                    LaunchedEffect(year, month) { monthViewModel.loadMonthData(YearMonth.of(year, month)) }
                    MonthDetailScreen(onBack = { navController.popBackStack() }, year = year, month = month, viewModel = monthViewModel, onSignInSuccess = { homeViewModel.onUserSignedIn() })
                }
            }
        }
    }

    // ── How Layzbug works — full screen dialog ───────────────────────
    if (showHowItWorks) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showHowItWorks = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            OnboardingScreen(viewOnly = true, onComplete = { showHowItWorks = false })
        }
    }

    // ── Drawer signed-in info sheet ────────────────────────────────────
    if (showDrawerInfoSheet) {
        DrawerSignedInInfoSheet(onClose = { showDrawerInfoSheet = false })
    }

    // ── Sync info sheet (from drawer cloud sync card) ────────────────
    if (showSyncInfoSheet) {
        DrawerSyncInfoSheet(
            onClose = { showSyncInfoSheet = false },
            onSignInClick = {
                showSyncInfoSheet = false
                signInLauncher.launch(authManager.getGoogleSignInClient().signInIntent)
            }
        )
    }

    // ── Hour picker dialog ───────────────────────────────────────────
    if (showTimePicker) {
        HourPickerDialog(
            currentHour = notifHour,
            onConfirm = { hour ->
                scope.launch { notificationPrefs.setHour(hour) }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

// ─── SECTION LABEL ───────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = Color.Black.copy(alpha = 0.35f),
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

// ─── SYNC INFO SHEET (drawer) ─────────────────────────────────────────

@Composable
private fun DrawerSyncInfoSheet(onClose: () -> Unit, onSignInClick: () -> Unit) {
    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onClose, lightBackground = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .background(RamsSurface, CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OrangeAccent))
                Text("CLOUD SYNC IS OFF", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = VictorMono, letterSpacing = 1.1.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text("Sign in to save your walks", color = HeadlineColor, fontSize = 18.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = (-0.3).sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Layzbug automatically tracks your 30 minute walks on this device. If you go for a walk without your phone, you can manually mark that day as walked.\n\nJust note, if not signed in, these marked days may not appear on other devices. Only the walks tracked automatically will show.",
                color = BodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp
            )
            Spacer(Modifier.height(28.dp))
            Text("SIGN IN IS OPTIONAL", color = OrangeAccent, fontSize = 13.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth().height(56.dp)
                    .clip(CircleShape).background(RamsSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    .clickable { onSignInClick() }.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogoDrawer(modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sign in to sync across devices", color = Color.White, fontSize = 14.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
            }
        }
    }
}

// ─── HOUR PICKER DIALOG ──────────────────────────────────────────────

@Composable
private fun HourPickerDialog(currentHour: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var selectedHour by remember { mutableIntStateOf(currentHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Reminder time", fontFamily = VictorMono, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HeadlineColor)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Large time display — Rams style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(RamsSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%02d:00".format(selectedHour),
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 44.sp,
                        color = OrangeAccent,
                        letterSpacing = (-1).sp
                    )
                }
                Text(
                    text = "Drag to set your preferred reminder time",
                    fontSize = 13.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Medium,
                    color = BodyTextMuted
                )
                Slider(
                    value = selectedHour.toFloat(),
                    onValueChange = { selectedHour = it.toInt() },
                    valueRange = 6f..22f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor             = OrangeAccent,
                        activeTrackColor       = OrangeAccent,
                        inactiveTrackColor     = Color.Black.copy(alpha = 0.1f),
                        activeTickColor        = Color.Transparent,
                        inactiveTickColor      = Color.Transparent
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("06:00", fontSize = 11.sp, fontFamily = JetBrainsMono, color = BodyTextMuted)
                    Text("22:00", fontSize = 11.sp, fontFamily = JetBrainsMono, color = BodyTextMuted)
                }
            }
        },
        confirmButton = {
            // Onboarding-style dark pill button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(RamsSurface)
                    .clickable { onConfirm(selectedHour) }
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SAVE", color = OrangeAccent, fontSize = 13.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                    .clickable { onDismiss() }
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("CANCEL", color = BodyTextMuted, fontSize = 13.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            }
        }
    )
}

// ─── DRAWER SIGNED-IN TOAST ─────────────────────────────────────────

@Composable
private fun DrawerSignedInToast(onDismiss: () -> Unit, onInfo: () -> Unit) {
    val ToastGreen      = Color(0xFF1A6E35)
    val ToastGreenLight = Color(0xFF2A9E50)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(ToastGreen)
            .border(1.dp, ToastGreenLight.copy(alpha = 0.4f), RoundedCornerShape(36.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close — left
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            // Text — centre
            Text(
                text = "Logged in.",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = VictorMono,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .height(28.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
            )
            // Info — right
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onInfo() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    color = ToastGreen,
                    fontSize = 16.sp,
                    fontFamily = VictorMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

// ─── DRAWER SIGNED-IN INFO SHEET ─────────────────────────────────────

@Composable
private fun DrawerSignedInInfoSheet(onClose: () -> Unit) {
    val GoalGreenText = Color(0xFF1A6E35)

    com.layzbug.app.ui.components.LayzbugBottomSheet(onClose = onClose, lightBackground = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 40.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .background(GoalGreenText, CircleShape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(GreenAccent))
                Text("CLOUD SYNC IS ON", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = VictorMono, letterSpacing = 1.1.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text("Your walks are saved", color = HeadlineColor, fontSize = 18.sp, fontFamily = VictorMono, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = (-0.3).sp)
            Spacer(Modifier.height(20.dp))
            Text("Your walks are now synced across all your devices. Whenever you log in to a new device using the same Google account, all your walks will be shown.", color = BodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            Spacer(Modifier.height(20.dp))
            Text("This includes walks that were automatically detected by the app, as well as any days you marked manually.", color = BodyTextMuted, fontSize = 15.sp, fontFamily = VictorMono, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
        }
    }
}

// ─── GOOGLE G LOGO ───────────────────────────────────────────────────

@Composable
private fun GoogleGLogoDrawer(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height)
        val strokeWidth = s * 0.22f
        val arcSize = Size(s - strokeWidth, s - strokeWidth)
        val arcTopLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val googleBlue = Color(0xFF4285F4); val googleRed = Color(0xFFEA4335)
        val googleYellow = Color(0xFFFBBC05); val googleGreen = Color(0xFF34A853)
        drawArc(color = googleRed,    startAngle = 195f, sweepAngle = 105f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawArc(color = googleYellow, startAngle = 135f, sweepAngle = 60f,  useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawArc(color = googleGreen,  startAngle = 45f,  sweepAngle = 90f,  useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawArc(color = googleBlue,   startAngle = -15f, sweepAngle = 105f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
        drawLine(color = googleBlue, start = Offset(s / 2, s / 2), end = Offset(s - strokeWidth, s / 2), strokeWidth = strokeWidth, cap = StrokeCap.Square)
    }
}

// ─── NAV VIEWMODEL ───────────────────────────────────────────────────

@dagger.hilt.android.lifecycle.HiltViewModel
class LayzbugNavViewModel @Inject constructor(
    val notificationPrefs: NotificationPrefsRepository
) : androidx.lifecycle.ViewModel()