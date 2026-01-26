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

// --- Font Family Definitions ---

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val DisplayRobotoFlex = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(55f),
            FontVariation.slant(-10f),
            FontVariation.Setting("opsz", 80f)
        )
    )
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val GoogleSansFlexMedium = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.Setting("opsz", 32f)
        )
    )
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val GoogleSansFlexRegular = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("opsz", 20f)
        )
    )
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val RobotoFlexRegular = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("opsz", 14f)
        )
    )
)

// Public aliases to fix "Unresolved reference" errors in external files
val GoogleSansFlex = GoogleSansFlexRegular
val RobotoFlex = RobotoFlexRegular

// --- Typography Theme ---

val AppTypography = Typography(
    // title-80-semibold (Figma: 365)
    displayLarge = TextStyle(
        fontFamily = DisplayRobotoFlex,
        fontWeight = FontWeight.W600,
        fontSize = 80.sp,
        lineHeight = 94.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center
    ),

    // title-32-medium (Figma: Hey Layzbug)
    headlineLarge = TextStyle(
        fontFamily = GoogleSansFlexMedium,
        fontWeight = FontWeight.W500,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    // title-20-medium (Figma: Log your walks)
    headlineMedium = TextStyle(
        fontFamily = GoogleSansFlexMedium,
        fontWeight = FontWeight.W500,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // title-16-medium (Figma: Walked)
    headlineSmall = TextStyle(
        fontFamily = GoogleSansFlexMedium,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    // title-24-regular (Figma: January 16)
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlexRegular,
        fontWeight = FontWeight.W400,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // title-22-regular (Figma: Layzbug)
    titleMedium = TextStyle(
        fontFamily = GoogleSansFlexRegular,
        fontWeight = FontWeight.W400,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // title-14-regular (Figma: Feb)
    titleSmall = TextStyle(
        fontFamily = GoogleSansFlexRegular,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),

    // body-large (Figma: Walks in January)
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlexRegular,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    // body-small (Figma: Progress text)
    bodySmall = TextStyle(
        fontFamily = RobotoFlexRegular,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
)