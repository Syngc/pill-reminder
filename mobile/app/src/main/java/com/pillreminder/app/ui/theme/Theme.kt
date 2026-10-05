package com.pillreminder.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pillreminder.app.R

/**
 * Colors from the MedRing redesign. Status is never told by color alone: every status pairs a
 * color with an icon and a word. The alarm and "well done" screens use the same strong colors in
 * light and dark mode.
 */
@Immutable
data class Palette(
    val background: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    /** Fill of the main buttons; white text on it. */
    val accent: Color,
    /** Accent for text and icons on [surface]. */
    val accentText: Color,
    val tint: Color,
    val chip: Color,
    val chipText: Color,
    val laterChip: Color,
    val laterText: Color,
    val warning: Color,
    val warningChip: Color,
    val warningText: Color,
    val warningBorder: Color,
    val danger: Color,
    val dangerBorder: Color,
    /** Dark button on the light background ("All my medicines"). */
    val strong: Color,
    val onStrong: Color,
    val alarmBackground: Color = Color(0xFF12306E),
    val alarmSubtle: Color = Color(0xFFC9D7F7),
    val alarmOutline: Color = Color(0xFF4E68A6),
    val doneBackground: Color = Color(0xFF1F47B8),
    val doneSubtle: Color = Color(0xFFDCE6FB),
)

val LightPalette = Palette(
    background = Color(0xFFF5F8FD),
    surface = Color(0xFFFFFFFF),
    ink = Color(0xFF0E1B2C),
    muted = Color(0xFF51606F),
    border = Color(0xFFCBD6E8),
    accent = Color(0xFF1F47B8),
    accentText = Color(0xFF1F47B8),
    tint = Color(0xFFE6EDFB),
    chip = Color(0xFFEDF2FC),
    chipText = Color(0xFF163587),
    laterChip = Color(0xFFF1F3F7),
    laterText = Color(0xFF3F4B59),
    warning = Color(0xFFFDF0E6),
    warningChip = Color(0xFFFCE3D0),
    warningText = Color(0xFF7A3606),
    warningBorder = Color(0xFFB4530F),
    danger = Color(0xFFA1281E),
    dangerBorder = Color(0xFFE2C9C4),
    strong = Color(0xFF0E1B2C),
    onStrong = Color(0xFFFFFFFF),
)

val DarkPalette = Palette(
    background = Color(0xFF0B1424),
    surface = Color(0xFF15233A),
    ink = Color(0xFFF2F5FA),
    muted = Color(0xFFA9B6C8),
    border = Color(0xFF34465F),
    accent = Color(0xFF3558C9),
    accentText = Color(0xFFA8C1FF),
    tint = Color(0xFF1E3159),
    chip = Color(0xFF22365E),
    chipText = Color(0xFFD3E0FF),
    laterChip = Color(0xFF26334A),
    laterText = Color(0xFFC8D2E0),
    warning = Color(0xFF3A2614),
    warningChip = Color(0xFF4A2E15),
    warningText = Color(0xFFFFD3AE),
    warningBorder = Color(0xFFE08A45),
    danger = Color(0xFFFF9C92),
    dangerBorder = Color(0xFF6B3A35),
    strong = Color(0xFFE8EEF8),
    onStrong = Color(0xFF0E1B2C),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** The current palette: `AppColors.current.accent`. */
object AppColors {
    val current: Palette
        @Composable get() = LocalPalette.current
}

@OptIn(ExperimentalTextApi::class)
private fun atkinson(weight: Int) = Font(
    R.font.atkinson_hyperlegible_next,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Atkinson Hyperlegible Next: designed for readers with low vision. */
val Atkinson = FontFamily(atkinson(400), atkinson(600), atkinson(700), atkinson(800))

// Nothing below 18sp; key information 26–72sp.
private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 72.sp, lineHeight = 76.sp),
    displayMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 60.sp, lineHeight = 64.sp),
    headlineLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 37.sp),
    headlineSmall = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 33.sp),
    titleLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 31.sp),
    titleMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleSmall = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
    bodySmall = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 25.sp),
    labelLarge = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
    labelMedium = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp),
    labelSmall = TextStyle(fontFamily = Atkinson, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp),
)

@Composable
fun PillReminderTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    val dark = palette == DarkPalette
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val colors = base.copy(
        primary = palette.accent,
        onPrimary = Color.White,
        primaryContainer = palette.tint,
        onPrimaryContainer = palette.chipText,
        secondary = palette.accentText,
        background = palette.background,
        onBackground = palette.ink,
        surface = palette.surface,
        onSurface = palette.ink,
        surfaceVariant = palette.chip,
        onSurfaceVariant = palette.muted,
        surfaceContainerHigh = palette.surface,
        surfaceContainerHighest = palette.surface,
        outline = palette.border,
        outlineVariant = palette.border,
        error = palette.danger,
        errorContainer = palette.warning,
        onErrorContainer = palette.warningText,
    )
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
    }
}
