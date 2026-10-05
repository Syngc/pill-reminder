package com.pillreminder.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val TakeGreen = Color(0xFF1B6E4F)
val TakeGreenDark = Color(0xFF7FD8AE)

private val LightColors = lightColorScheme(
    primary = TakeGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEFDD),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFFAFDF9),
    surface = Color(0xFFFAFDF9),
)

private val DarkColors = darkColorScheme(
    primary = TakeGreenDark,
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005237),
    onPrimaryContainer = Color(0xFFCDEFDD),
)

// Larger than Material defaults throughout: the people using this app often have low vision.
private val LargeTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontSize = 22.sp),
        bodyLarge = bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
        bodyMedium = bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
        labelLarge = labelLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    )
}

val HugeButtonText = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)

@Composable
fun PillReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = LargeTypography,
        content = content,
    )
}
