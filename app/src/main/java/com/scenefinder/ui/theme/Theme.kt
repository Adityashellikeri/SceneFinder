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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.scenefinder.data.prefs.ThemeMode

// ─────────────────────────────────────────────────────────────────────────────
// AppColors – Cyberpunk dynamic color token provider
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
    isDark          = true,
    primary         = NeonCyan,
    background      = CyberBlack,
    surface         = CyberSurface,
    card            = CyberCard,
    cardElevated    = CyberCardElev,
    divider         = CyberDivider,
    textPrimary     = CyberTextPrimary,
    textSecondary   = CyberTextSecondary,
    textTertiary    = CyberTextTertiary,
    accent          = NeonMagenta
)

val LightAppColors = AppColors(
    isDark          = false,
    primary         = CyberBlue,
    background      = CyberLightBg,
    surface         = CyberLightSurface,
    card            = CyberLightCard,
    cardElevated    = CyberLightCardElev,
    divider         = CyberLightDivider,
    textPrimary     = CyberLightTextPri,
    textSecondary   = CyberLightTextSec,
    textTertiary    = CyberLightTextTer,
    accent          = CyberGradB
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ─────────────────────────────────────────────────────────────────────────────
// Material ColorSchemes
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary             = NeonCyan,
    onPrimary           = CyberBlack,
    primaryContainer    = CyberCard,
    onPrimaryContainer  = CyberTextPrimary,
    secondary           = NeonMagenta,
    onSecondary         = CyberBlack,
    tertiary            = NeonViolet,
    onTertiary          = Color.White,
    error               = CyberRed,
    onError             = Color.White,
    background          = CyberBlack,
    onBackground        = CyberTextPrimary,
    surface             = CyberSurface,
    onSurface           = CyberTextPrimary,
    surfaceVariant      = CyberCard,
    onSurfaceVariant    = CyberTextSecondary,
    outline             = CyberDivider,
    outlineVariant      = CyberTextTertiary,
    inversePrimary      = NeonGreenLime
)

private val LightColorScheme = lightColorScheme(
    primary             = CyberBlue,
    onPrimary           = Color.White,
    primaryContainer    = CyberLightCard,
    onPrimaryContainer  = CyberLightTextPri,
    secondary           = CyberGradB,
    onSecondary         = Color.White,
    error               = CyberRed,
    onError             = Color.White,
    background          = CyberLightBg,
    onBackground        = CyberLightTextPri,
    surface             = CyberLightSurface,
    onSurface           = CyberLightTextPri,
    surfaceVariant      = CyberLightCard,
    onSurfaceVariant    = CyberLightTextSec,
    outline             = CyberLightDivider,
    outlineVariant      = CyberLightTextTer
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography – clean modern monospace-influenced type scale
// ─────────────────────────────────────────────────────────────────────────────

private fun buildTypography(isDark: Boolean): Typography {
    val textPrim = if (isDark) CyberTextPrimary else CyberLightTextPri
    val textSec  = if (isDark) CyberTextSecondary else CyberLightTextSec
    return Typography(
        displayLarge = TextStyle(
            fontFamily  = FontFamily.Default,
            fontWeight  = FontWeight.Black,
            fontSize    = 57.sp,
            lineHeight  = 64.sp,
            letterSpacing = (-0.25).sp,
            color       = textPrim
        ),
        headlineLarge = TextStyle(
            fontWeight  = FontWeight.Bold,
            fontSize    = 26.sp,
            lineHeight  = 32.sp,
            letterSpacing = (-0.5).sp,
            color       = textPrim
        ),
        headlineMedium = TextStyle(
            fontWeight  = FontWeight.Bold,
            fontSize    = 22.sp,
            lineHeight  = 28.sp,
            letterSpacing = (-0.3).sp,
            color       = textPrim
        ),
        titleLarge = TextStyle(
            fontWeight  = FontWeight.SemiBold,
            fontSize    = 18.sp,
            lineHeight  = 24.sp,
            color       = textPrim
        ),
        titleMedium = TextStyle(
            fontWeight  = FontWeight.SemiBold,
            fontSize    = 15.sp,
            lineHeight  = 20.sp,
            letterSpacing = 0.1.sp,
            color       = textPrim
        ),
        bodyLarge = TextStyle(
            fontWeight  = FontWeight.Normal,
            fontSize    = 14.sp,
            lineHeight  = 20.sp,
            color       = textPrim
        ),
        bodyMedium = TextStyle(
            fontWeight  = FontWeight.Normal,
            fontSize    = 13.sp,
            lineHeight  = 18.sp,
            color       = textPrim
        ),
        bodySmall = TextStyle(
            fontWeight  = FontWeight.Normal,
            fontSize    = 12.sp,
            lineHeight  = 16.sp,
            color       = textSec
        ),
        labelLarge = TextStyle(
            fontWeight  = FontWeight.SemiBold,
            fontSize    = 13.sp,
            letterSpacing = 0.4.sp,
            color       = textPrim
        ),
        labelSmall = TextStyle(
            fontWeight  = FontWeight.Medium,
            fontSize    = 10.sp,
            letterSpacing = 0.8.sp,
            color       = textSec
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
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
    }

    val appColors    = if (isDark) DarkAppColors  else LightAppColors
    val colorScheme  = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = buildTypography(isDark),
            content     = content
        )
    }
}
