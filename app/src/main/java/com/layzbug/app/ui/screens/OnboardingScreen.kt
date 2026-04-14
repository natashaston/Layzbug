package com.layzbug.app.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.layzbug.app.R
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.zIndex

// ─── FONTS ──────────────────────────────────────────────────────────

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal)
)

private val VictorMono = FontFamily(
    Font(R.font.victor_mono_regular, FontWeight.Normal),
    Font(R.font.victor_mono_medium, FontWeight.Medium),
    Font(R.font.victor_mono_bold, FontWeight.Bold)
)

// ─── PALETTE ────────────────────────────────────────────────────────

private val RamsSurface = Color(0xFF151619)
private val RamsBorder = Color.White.copy(alpha = 0.05f)
private val RamsTextMuted = Color.White.copy(alpha = 0.6f)
private val RamsGridLine = Color.Gray.copy(alpha = 0.03f)
private val RamsChipBg = Color.White.copy(alpha = 0.03f)
private val OrangeAccent = Color(0xFFFF4400)
private val GreenAccent = Color(0xFF00FF66)
private val RedAccent = Color(0xFFEF4444)

// ─── MAIN SCREEN ────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 6

    // Permission launcher — triggers on GET STARTED
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        Log.d("Onboarding", "Granted permissions: $grantedPermissions")
        onComplete()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Page indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(
                                width = if (index == currentPage) 24.dp else 6.dp,
                                height = 6.dp
                            )
                            .clip(if (index == currentPage) RoundedCornerShape(3.dp) else CircleShape)
                            .background(
                                if (index == currentPage) OrangeAccent
                                else Color.Black.copy(alpha = 0.1f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Content
            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val gap = 0.15f
                    if (targetState > initialState) {
                        slideInHorizontally { (it * (1 + gap)).toInt() } togetherWith
                                slideOutHorizontally { -(it * (1 + gap)).toInt() }
                    } else {
                        slideInHorizontally { -(it * (1 + gap)).toInt() } togetherWith
                                slideOutHorizontally { (it * (1 + gap)).toInt() }
                    }
                },
                label = "onboarding"
            ) { page ->
                when (page) {
                    0 -> PageHook()
                    1 -> PageGoal()
                    2 -> PageSmartDetection()
                    3 -> PageHowItWorks()
                    4 -> PageNotification()
                    5 -> PagePermissions()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom nav
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        shape = CircleShape,
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.Black.copy(alpha = 0.1f))
                        ),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "BACK",
                            fontSize = 12.sp,
                            fontFamily = VictorMono,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage++
                        } else {
                            requestPermissionLauncher.launch(permissions)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RamsSurface
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = if (currentPage < totalPages - 1) "NEXT" else "GET STARTED",
                        color = OrangeAccent,
                        fontSize = 13.sp,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    )
                }
            }
        }
    }
}

// ─── PAGE 1: THE HOOK + CORE RULE ───────────────────────────────────

@Composable
private fun PageHook() {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_layzbug),
            contentDescription = null,
            modifier = Modifier.size(140.dp).offset(y = floatingOffset.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "Science says 30 minutes of intentional walking changes everything.\n\nLayzbug helps you prove it.",
            textAlign = TextAlign.Center,
            fontFamily = VictorMono,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            color = Color.Black.copy(alpha = 0.7f)
        )
    }
}

// ─── PAGE 2: THE GOAL ───────────────────────────────────

@Composable
private fun PageGoal() {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_emojis")

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your Daily Objective",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            fontFamily = VictorMono,
            color = Color.Black.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 2. The Circular Hero Area
        Box(contentAlignment = Alignment.Center) {

            // Central Circular RamsCard
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(RamsSurface)
                    .border(1.dp, RamsBorder, CircleShape)
                    .drawBehind {
                        val gridSize = 4.dp.toPx()
                        for (x in 0..size.width.toInt() step gridSize.toInt()) {
                            drawLine(
                                RamsGridLine,
                                Offset(x.toFloat(), 0f),
                                Offset(x.toFloat(), size.height),
                                1.dp.toPx()
                            )
                        }
                        for (y in 0..size.height.toInt() step gridSize.toInt()) {
                            drawLine(
                                RamsGridLine,
                                Offset(0f, y.toFloat()),
                                Offset(size.width, y.toFloat()),
                                1.dp.toPx()
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "30",
                        color = OrangeAccent,
                        fontSize = 100.sp,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-4).sp,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = OrangeAccent.copy(alpha = 0.5f),
                                offset = Offset.Zero,
                                blurRadius = 50f
                            )
                        )
                    )
                    Text(
                        text = "MINUTES OF",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "WALKING",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Pendulum sweep — each element swings individually along the bottom arc
            val pendulumAngle by infiniteTransition.animateFloat(
                initialValue = 95f,   // bottom-left of circle
                targetValue = 85f,     // bottom-right of circle
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pendulum"
            )

            val orbitRadius = 200f

            // Each element at its own angle offset — creates curved chain along arc
            PendulumElement(angle = pendulumAngle + 12f, radius = orbitRadius) {
                Text(text = "🙅🏼", fontSize = 28.sp)
            }
            PendulumElement(angle = pendulumAngle, radius = orbitRadius) {
                Text(text = "🐂", fontSize = 28.sp)
            }
            PendulumElement(angle = pendulumAngle - 12f, radius = orbitRadius) {
                Text(text = "💩", fontSize = 28.sp)
            }

        }
    }
}

@Composable
private fun BoxScope.PendulumElement(
    angle: Float,
    radius: Float,
    content: @Composable () -> Unit
) {
    val angleRad = Math.toRadians(angle.toDouble())
    val offsetX = (kotlin.math.cos(angleRad) * radius).toFloat()
    val offsetY = (kotlin.math.sin(angleRad) * radius).toFloat()

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = offsetX.dp, y = offsetY.dp)
            .graphicsLayer {
                rotationZ = angle - 90f  // keep tangent to circle
            }
    ) {
        content()
    }
}

// ─── PAGE 3: SMART DETECTION ────────────────────────────────────────

@Composable
private fun PageSmartDetection() {
    data class RainChip(
        val text: String,
        val isValid: Boolean,
        val column: Int,
        val duration: Int,
        val delay: Int
    )

    val chips = remember {
        listOf(
            RainChip("5 MINS", true, 0, 4000, 0),
            RainChip("3 MINS", false, 1, 4500, 600),
            RainChip("8 MINS", true, 2, 3800, 300),
            RainChip("2 MINS", false, 3, 4200, 900),
            RainChip("12 MINS", true, 1, 4300, 1500),
            RainChip("4 MINS", false, 0, 3900, 1200),
            RainChip("6 MINS", true, 3, 4400, 2000),
            RainChip("1 MIN", false, 2, 4100, 1800),
            RainChip("15 MINS", true, 0, 4200, 2500),
            RainChip("7 MINS", true, 2, 4000, 2800),
            RainChip("4 MINS", false, 1, 4300, 3200),
            RainChip("10 MINS", true, 3, 3900, 3500),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rain")

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Title Section
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Not all minutes count",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                fontFamily = VictorMono,
                color = Color.Black.copy(0.8f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            " WALKS UNDER 5 MINUTES ARE FILTERED OUT",
            textAlign = TextAlign.Center,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = OrangeAccent
        )
        Spacer(modifier = Modifier.height(40.dp))
        // The Oval Rain Container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)       // takes 85% of available width
                .aspectRatio(1f)            // forces height = width = perfect square
                .clip(CircleShape)
                .background(RamsSurface)
                .border(1.dp, RamsBorder, CircleShape)
                .drawBehind {
                    val gridSize = 4.dp.toPx()
                    for (x in 0..size.width.toInt() step gridSize.toInt()) {
                        drawLine(
                            RamsGridLine,
                            Offset(x.toFloat(), 0f),
                            Offset(x.toFloat(), size.height),
                            1.dp.toPx()
                        )
                    }
                    for (y in 0..size.height.toInt() step gridSize.toInt()) {
                        drawLine(
                            RamsGridLine,
                            Offset(0f, y.toFloat()),
                            Offset(size.width, y.toFloat()),
                            1.dp.toPx()
                        )
                    }
                }
        ) {
            chips.forEach { chip ->
                val fallY by infiniteTransition.animateFloat(
                    initialValue = -100f,
                    targetValue = 460f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(chip.duration, chip.delay, LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "fall"
                )

                val xFraction = when (chip.column) {
                    0 -> 0.15f
                    1 -> 0.40f
                    2 -> 0.65f
                    3 -> 0.85f
                    else -> 0.5f
                }

                // Kill zone for orange chips — explodes between Y=180 and Y=230
                val killStart = 100f
                val killEnd = 200f
                val killProgress = if (!chip.isValid && fallY > killStart) {
                    ((fallY - killStart) / (killEnd - killStart)).coerceIn(0f, 1f)
                } else 0f

                val isExploding = !chip.isValid && killProgress in 0.01f..0.99f
                val isDead = !chip.isValid && killProgress >= 0.99f

                // Hide chip text once explosion starts
                val chipAlpha = when {
                    chip.isValid -> 1f
                    killProgress < 0.1f -> 1f
                    else -> 0f
                }

                // Render the chip text
                if (!isDead) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = fallY.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(BiasAlignment(xFraction * 2 - 1, 0f))
                                .graphicsLayer { alpha = chipAlpha }
                        ) {
                            RainChipPill(text = chip.text, isValid = chip.isValid)
                        }
                    }
                }

                // Render explosion particles for orange chips
                if (isExploding) {
                    ExplosionParticles(
                        chipKey = "${chip.text}_${chip.column}_${chip.delay}",
                        xFraction = xFraction,
                        yOffset = fallY,
                        progress = killProgress
                    )
                }
            }
        }

        // Footer Copy
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Only intentional walks count.\nShorter walks to bathroom,\nkitchen, and in between rooms,\nare filtered out.",
            textAlign = TextAlign.Center,
            fontFamily = VictorMono,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            color = Color.Black.copy(0.6f)
        )
    }
}
@Composable
private fun RainChipPill(text: String, isValid: Boolean) {
    val textColor = if (isValid) GreenAccent else OrangeAccent
    Text(
        text = text,
        color = textColor,
        fontSize = 13.sp,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = textColor.copy(alpha = 0.8f),
                offset = Offset.Zero,
                blurRadius = 0f
            )
        )
    )
}

@Composable
private fun BoxScope.ExplosionParticles(
    chipKey: String,
    xFraction: Float,
    yOffset: Float,
    progress: Float
) {
    // Generate stable random particles per chip
    val particles = remember(chipKey) {
        val random = kotlin.random.Random(chipKey.hashCode())
        List(50) {
            ParticleData(
                angle = random.nextFloat() * 2f * Math.PI.toFloat(),
                speed = 20f + random.nextFloat() * 60f,      // 20-80 dp travel
                size = 0.4f + random.nextFloat() * 1.1f,      // 0.8-3.0 dp radius
                gravity = 30f + random.nextFloat() * 40f,     // 30-70 downward pull
                lifeDecay = 0.7f + random.nextFloat() * 0.3f, // 0.7-1.0 fade rate
                colorMix = random.nextFloat()                  // 0=orange, 1=yellow
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (yOffset - 90f).dp)  // shift up by ~half the canvas height
    ) {
        Box(modifier = Modifier.align(BiasAlignment(xFraction * 2 - 1, 0f))) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val centerX = size.width / 2
                val centerY = size.height / 2

                particles.forEach { p ->
                    // Position: radial + gravity arc
                    val t = progress
                    val px = centerX + kotlin.math.cos(p.angle) * p.speed * t * density
                    val py = centerY + kotlin.math.sin(p.angle) * p.speed * t * density +
                            (p.gravity * t * t * density)  // gravity accelerates

                    // Alpha fades with custom decay curve
                    val alpha = (1f - (t / p.lifeDecay).coerceAtMost(1f)).coerceAtLeast(0f)
                    if (alpha <= 0f) return@forEach

                    // Mix between orange and yellow
                    val color = androidx.compose.ui.graphics.lerp(
                        OrangeAccent,
                        Color(0xFFFFAA00),
                        p.colorMix
                    ).copy(alpha = alpha)

                    // Shrink slightly as it fades
                    val radius = (p.size * (1f - t * 0.3f)) * density

                    drawCircle(
                        color = color,
                        radius = radius.coerceAtLeast(0.5f),
                        center = Offset(px, py)
                    )
                }

            }
        }
    }
}

private data class ParticleData(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val gravity: Float,
    val lifeDecay: Float,
    val colorMix: Float
)

// ─── PAGE 4: HOW IT WORKS ───────────────────────────────────────────

@Composable
private fun PageHowItWorks() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),  // page-level padding
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Zero manual tracking",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = VictorMono,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "WE SYNC WITH GOOGLE FIT\nIN THE BACKGROUND",
            textAlign = TextAlign.Center,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = OrangeAccent
        )
        Spacer(modifier = Modifier.height(40.dp))

        OrbitingOrb(
            modifier = Modifier
                .fillMaxWidth(0.85f)   // matches PageSmartDetection
                .aspectRatio(1f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Your only job is to move. We handle the math and the calendar.",
            modifier = Modifier.padding(horizontal = 24.dp), // Extra padding for better text wrapping
            textAlign = TextAlign.Center,
            fontFamily = VictorMono,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black.copy(0.6f)
        )
    }
}

private data class OrbitParticle(
    val latitude: Float,   // -90..90, vertical band
    val longitude: Float,  // 0..360, starting angle
    val speed: Float,      // rotation speed multiplier
    val size: Float,       // base radius in dp
    val colorMix: Float    // 0=orange, 1=yellow
)

@Composable
private fun OrbitingOrb(modifier: Modifier = Modifier) {
    // Generate stable particles once
    val particles = remember {
        val random = kotlin.random.Random(42)
        List(180) {
            OrbitParticle(
                latitude = (random.nextFloat() - 0.5f) * 160f,    // -80..80
                longitude = random.nextFloat() * 360f,
                speed = 0.6f + random.nextFloat() * 0.8f,         // 0.6-1.4x
                size = 0.8f + random.nextFloat() * 1.4f,          // 0.8-2.2 dp
                colorMix = random.nextFloat()
            )
        }
    }

    // One global rotation animator drives all particles
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val globalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(RamsSurface)
            .border(1.dp, RamsBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val orbRadius = size.minDimension / 2 * 0.92f  // 75% of circle radius

            particles.forEach { p ->
                // Current longitude = starting + global rotation * speed
                val lonRad = Math.toRadians((p.longitude + globalRotation * p.speed).toDouble())
                val latRad = Math.toRadians(p.latitude.toDouble())

                // Spherical → 2D projection
                val x = (kotlin.math.cos(latRad) * kotlin.math.sin(lonRad)).toFloat()
                val y = (kotlin.math.sin(latRad)).toFloat()
                val z = (kotlin.math.cos(latRad) * kotlin.math.cos(lonRad)).toFloat()

                // Depth: z > 0 means in front, z < 0 behind
                // Scale size and alpha by depth for fake 3D
                val depthFactor = (z + 1f) / 2f  // 0..1
                val particleAlpha = 0.3f + depthFactor * 0.7f
                val particleSize = (p.size * (0.5f + depthFactor * 0.5f)) * density

                val px = centerX + x * orbRadius
                val py = centerY + y * orbRadius

                // Color — lerp orange to yellow
                val color = androidx.compose.ui.graphics.lerp(
                    OrangeAccent,
                    Color(0xFFFFAA00),
                    p.colorMix
                ).copy(alpha = particleAlpha)

                // Draw particle dot
                drawCircle(
                    color = color,
                    radius = particleSize,
                    center = Offset(px, py)
                )
            }
        }

        // Central label
        Text(
            text = "AUTO",
            color = Color.White.copy(0.0f),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 2.sp
        )
    }
}

// ─── PAGE 5: NOTIFICATION ───────────────────────────────────────────

@Composable
private fun PageNotification() {
    Column(
        modifier = Modifier.fillMaxSize(), // Removed horizontal padding here
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title - Padding added here instead
        Text(
            text = "Silence is earned",
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color.Black.copy(alpha = 0.8f),
            fontSize = 20.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "ZERO SPAM",
            textAlign = TextAlign.Center,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = OrangeAccent
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "You only get one notification a day, and only if you haven't walked for the day.",
            modifier = Modifier.padding(horizontal = 24.dp), // Extra padding for better text wrapping
            textAlign = TextAlign.Center,
            fontFamily = VictorMono,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black.copy(0.6f)
        )

        Spacer(modifier = Modifier.height(40.dp))
        // Card - Now has full access to the screen width
        RamsCard {
            Column(modifier = Modifier.padding(20.dp)) {
                ChipLabel(text = "NOTIFICATION", color = OrangeAccent)

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(OrangeAccent)
                            )
                            Text(
                                text = "LAYZBUG",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontFamily = VictorMono,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "18:30",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMono
                            )
                        }
                        Text(
                            text = "No walk detected today.\nGet your ass moving! 🍑",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontFamily = VictorMono,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

    }
}

// ─── PAGE 6: PERMISSIONS ────────────────────────────────────────────

@Composable
private fun PagePermissions() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Almost there",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = VictorMono,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "PERMISSIONS",
            textAlign = TextAlign.Center,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = OrangeAccent
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Grant access to send you notifications, read your steps and walking data from Google Fit.",
            modifier = Modifier.padding(horizontal = 24.dp), // Extra padding for better text wrapping
            textAlign = TextAlign.Center,
            fontFamily = VictorMono,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black.copy(0.6f)
        )
    }
}

// ─── SHARED COMPONENTS ──────────────────────────────────────────────

@Composable
private fun RamsCard(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RamsSurface)
            .border(1.dp, RamsBorder, RoundedCornerShape(24.dp))
            .drawBehind {
                val gridSize = 4.dp.toPx()
                for (x in 0..size.width.toInt() step gridSize.toInt()) {
                    drawLine(RamsGridLine, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.dp.toPx())
                }
                for (y in 0..size.height.toInt() step gridSize.toInt()) {
                    drawLine(RamsGridLine, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.dp.toPx())
                }
            }
    ) {
        content()
    }
}

@Composable
private fun ChipLabel(text: String, color: Color) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .background(RamsChipBg, CircleShape)
            .border(1.dp, RamsBorder, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontFamily = VictorMono,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun DetectionRow(text: String, isValid: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = if (isValid) "✓" else "✗",
            color = if (isValid) GreenAccent else OrangeAccent,
            fontSize = 16.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            color = RamsTextMuted,
            fontSize = 12.sp,
            fontFamily = VictorMono,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun FeatureCard(number: String, title: String, description: String) {
    RamsCard {
            Column(modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = description,
                    color = RamsTextMuted,
                    fontSize = 12.sp,
                    fontFamily = VictorMono,
                    lineHeight = 20.sp
                )
            }
        }
    }
