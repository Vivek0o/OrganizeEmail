package com.codeSmithLabs.organizeemail.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.codeSmithLabs.organizeemail.R

// TODO: 1. Add your font files (e.g., my_font_regular.ttf, my_font_bold.ttf) to app/src/main/res/font/
// TODO: 2. Uncomment the following AppFontFamily definition and comment out the fallback one.

val AppFontFamily = FontFamily(
    Font(R.font.edusahand_regular, FontWeight.Normal),
    Font(R.font.edusahand_bold, FontWeight.Bold),
    Font(R.font.edusahand_medium, FontWeight.Medium),
    Font(R.font.edusahand_semibold, FontWeight.SemiBold)
)

// Fallback until fonts are added
// val AppFontFamily = FontFamily.Default

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)