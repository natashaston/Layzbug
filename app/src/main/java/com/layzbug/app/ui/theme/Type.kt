package com.layzbug.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.layzbug.app.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val DisplayRobotoFlex = FontFamily(
    Font(
        resId = R.font.roboto_flex, // variable TTF
        variationSettings = FontVariation.Settings(
            FontVariation.weight(560),      // Figma weight
            FontVariation.width(55f),       // Figma Width
            FontVariation.slant(-10f),      // Figma Slant
            FontVariation.Setting("opsz", 36f) // Figma Optical size
        )
    )
)

// Roboto Flex (variable font)
val RobotoFlex = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.W400
    ),
    Font(
        resId = R.font.roboto_flex,
        weight = FontWeight.W600
    )
)

// Google Sans Flex (variable font)
val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.W400
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.W500
    )
)

val AppTypography = Typography(

    displayLarge = TextStyle(
        fontFamily = DisplayRobotoFlex,   // ← use the configured variable font
        fontWeight = FontWeight(570),
        fontSize = 80.sp,
        lineHeight = 94.sp,
        letterSpacing = -0.75.sp,
        fontFeatureSettings = "tnum",
        textAlign = TextAlign.Center
    ),

    bodySmall = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    headlineLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight(500),
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight(500),
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight(500),
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    titleLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight(400),
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),

    titleMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight(400),
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),

    titleSmall = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontWeight = FontWeight(400),
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
)
