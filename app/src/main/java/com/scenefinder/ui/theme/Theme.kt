package com.scenefinder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.scenefinder.data.prefs.ThemeMode

// ─────────────────────────────────────────────────────────────────────────────
// AppColors – Single source of truth for dynamic Dark/Light colors
// ─────────────────────────────────────────────────────────────────────────────

data class AppColors(
    val isDark: Boolean,
    val primary: Color,
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color
)

val DarkAppColors = AppColors(
    isDark = true,
    primary = IGBlue,
    background = IGBlack,
    surface = IGSurface,
    card = IGCard,
    cardElevated = IGCardElevated,
    divider = IGDivider,
    textPrimary = IGTextPrimary,
    textSecondary = IGTextSecondary,
    textTertiary = IGTextTertiary,
    accent = IGGradMid
)

val LightAppColors = AppColors(
    isDark = false,
    primary = IGBlue,
    background = IGLightBg,
    surface = IGLightSurface,
    card = IGLightCard,
    cardElevated = IGLightCardElevated,
    divider = IGLightDivider,
    textPrimary = IGLightTextPrimary,
    textSecondary = IGLightTextSecondary,
    textTertiary = IGLightTextTertiary,
    accent = IGGradMid
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ─────────────────────────────────────────────────────────────────────────────
// Material ColorSchemes
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = IGBlue,
    onPrimary = Color.White,
    primaryContainer = IGCard,
    onPrimaryContainer = IGTextPrimary,
    secondary = IGGradMid,
    onSecondary = Color.White,
    error = IGRed,
    onError = Color.White,
    background = IGBlack,
    onBackground = IGTextPrimary,
    surface = IGSurface,
    onSurface = IGTextPrimary,
    surfaceVariant = IGCard,
    onSurfaceVariant = IGTextSecondary,
    outline = IGDivider,
    outlineVariant = IGTextTertiary
)

private val LightColorScheme = lightColorScheme(
    primary = IGBlue,
    onPrimary = Color.White,
    primaryContainer = IGLightCard,
    onPrimaryContainer = IGLightTextPrimary,
    secondary = IGGradMid,
    onSecondary = Color.White,
    error = IGRed,
    onError = Color.White,
    background = IGLightBg,
    onBackground = IGLightTextPrimary,
    surface = IGLightSurface,
    onSurface = IGLightTextPrimary,
    surfaceVariant = IGLightCard,
    onSurfaceVariant = IGLightTextSecondary,
    outline = IGLightDivider,
    outlineVariant = IGLightTextTertiary
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography
// ─────────────────────────────────────────────────────────────────────────────

private fun buildTypography(isDark: Boolean): Typography {
    val textPrim = if (isDark) IGTextPrimary else IGLightTextPrimary
    val textSec = if (isDark) IGTextSecondary else IGLightTextSecondary
    return Typography(
        displayLarge = TextStyle(
            fontWeight = FontWeight.Black,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            color = textPrim
        ),
        headlineLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            color = textPrim
        ),
        headlineMedium = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = textPrim
        ),
        titleLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = textPrim
        ),
        titleMedium = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = textPrim
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = textPrim
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = textPrim
        ),
        bodySmall = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = textSec
        ),
        labelLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = textPrim
        ),
        labelSmall = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = textSec
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SceneFinderTheme
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SceneFinderTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val appColors = if (isDark) DarkAppColors else LightAppColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildTypography(isDark),
            content = content
        )
    }
}
