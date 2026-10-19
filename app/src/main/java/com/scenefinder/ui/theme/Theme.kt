package com.scenefinder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.scenefinder.data.prefs.AppTheme

// ─────────────────────────────────────────────────────────────────────────────
// AppThemeColors – the single source of truth for every dynamic color in the app
// ─────────────────────────────────────────────────────────────────────────────

data class AppThemeColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textInverse: Color,
    val shadowColor: Color = Color.Black,
    val isCyberpunk: Boolean = false,
    val isIronMan: Boolean = false,
    val isBrutalist: Boolean = false,
    val isDarkMinimal: Boolean = false,
    val navAccent: Color,
    val navAccentText: Color
)

// ─────────────────────────────────────────────────────────────────────────────
// Theme factory
// ─────────────────────────────────────────────────────────────────────────────

fun getThemeColors(theme: AppTheme): AppThemeColors = when (theme) {

    AppTheme.NEO_BRUTALISM -> AppThemeColors(
        primary         = BrutalYellow,
        secondary       = BrutalCyan,
        tertiary        = BrutalLime,
        accent          = BrutalPink,
        background      = BrutalDarkBg,
        surface         = BrutalCard,
        surfaceElevated = BrutalCardElevated,
        border          = BrutalBorder,
        borderMuted     = BrutalBorderMuted,
        textPrimary     = BrutalTextPrimary,
        textSecondary   = BrutalTextSecondary,
        textInverse     = BrutalTextInverse,
        shadowColor     = BrutalBlack,
        isBrutalist     = true,
        navAccent       = BrutalYellow,
        navAccentText   = BrutalTextInverse
    )

    AppTheme.CYBERPUNK_2077 -> AppThemeColors(
        primary         = CyberpunkYellow,
        secondary       = CyberpunkCyan,
        tertiary        = CyberpunkGreen,
        accent          = CyberpunkRed,
        background      = CyberpunkDarkBg,
        surface         = CyberpunkCard,
        surfaceElevated = CyberpunkCardElevated,
        border          = CyberpunkYellow,
        borderMuted     = Color(0xFF2A2A18),
        textPrimary     = Color(0xFFF0F0F0),
        textSecondary   = Color(0xFF9FA8A3),
        textInverse     = Color(0xFF08080C),
        shadowColor     = CyberpunkRed.copy(alpha = 0.8f),
        isCyberpunk     = true,
        navAccent       = CyberpunkYellow,
        navAccentText   = Color(0xFF08080C)
    )

    AppTheme.IRON_MAN -> AppThemeColors(
        primary         = IronManGold,
        secondary       = IronManBlueBright,
        tertiary        = IronManGoldBright,
        accent          = IronManRedBright,
        background      = IronManDarkBg,
        surface         = IronManCard,
        surfaceElevated = IronManCardElevated,
        border          = IronManBorder,
        borderMuted     = IronManBorderMuted,
        textPrimary     = Color(0xFFEEEEEE),
        textSecondary   = Color(0xFF9098A0),
        textInverse     = Color(0xFF060608),
        shadowColor     = IronManRed.copy(alpha = 0.6f),
        isIronMan       = true,
        navAccent       = IronManGold,
        navAccentText   = Color(0xFF060608)
    )

    AppTheme.DARK_MINIMAL -> AppThemeColors(
        primary         = Color(0xFF6C63FF),
        secondary       = Color(0xFF03DAC5),
        tertiary        = Color(0xFF4CAF50),
        accent          = Color(0xFFCF6679),
        background      = Color(0xFF000000),
        surface         = Color(0xFF121212),
        surfaceElevated = Color(0xFF1E1E1E),
        border          = Color(0xFF2C2C2C),
        borderMuted     = Color(0xFF1A1A1A),
        textPrimary     = Color(0xFFEEEEEE),
        textSecondary   = Color(0xFF888888),
        textInverse     = Color(0xFF000000),
        isDarkMinimal   = true,
        navAccent       = Color(0xFF6C63FF),
        navAccentText   = Color(0xFFFFFFFF)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocal
// ─────────────────────────────────────────────────────────────────────────────

val LocalAppThemeColors = staticCompositionLocalOf { getThemeColors(AppTheme.NEO_BRUTALISM) }

// ─────────────────────────────────────────────────────────────────────────────
// Material color scheme helper
// ─────────────────────────────────────────────────────────────────────────────

private fun buildMaterialColorScheme(tc: AppThemeColors) = darkColorScheme(
    primary              = tc.primary,
    onPrimary            = tc.textInverse,
    primaryContainer     = tc.surface,
    onPrimaryContainer   = tc.textPrimary,
    secondary            = tc.secondary,
    onSecondary          = tc.textPrimary,
    error                = tc.accent,
    onError              = tc.textPrimary,
    background           = tc.background,
    onBackground         = tc.textPrimary,
    surface              = tc.surface,
    onSurface            = tc.textPrimary,
    surfaceVariant       = tc.surfaceElevated,
    onSurfaceVariant     = tc.textSecondary,
    outline              = tc.borderMuted,
    outlineVariant       = tc.textSecondary
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography helper
// ─────────────────────────────────────────────────────────────────────────────

private fun buildTypography(tc: AppThemeColors): Typography {
    val titleFamily = if (tc.isCyberpunk || tc.isIronMan) FontFamily.Monospace else FontFamily.Default
    return Typography(
        displayLarge = TextStyle(
            fontFamily   = titleFamily,
            fontWeight   = FontWeight.Black,
            fontSize     = 57.sp,
            lineHeight   = 64.sp,
            color        = tc.textPrimary
        ),
        headlineLarge = TextStyle(
            fontFamily   = titleFamily,
            fontWeight   = FontWeight.Bold,
            fontSize     = 26.sp,
            lineHeight   = 32.sp,
            color        = tc.textPrimary
        ),
        headlineMedium = TextStyle(
            fontFamily   = titleFamily,
            fontWeight   = FontWeight.Bold,
            fontSize     = 22.sp,
            lineHeight   = 28.sp,
            color        = tc.textPrimary
        ),
        titleLarge = TextStyle(
            fontFamily   = titleFamily,
            fontWeight   = FontWeight.SemiBold,
            fontSize     = 18.sp,
            lineHeight   = 24.sp,
            color        = tc.textPrimary
        ),
        titleMedium = TextStyle(
            fontWeight   = FontWeight.SemiBold,
            fontSize     = 15.sp,
            lineHeight   = 20.sp,
            color        = tc.textPrimary
        ),
        bodyLarge = TextStyle(
            fontWeight   = FontWeight.Normal,
            fontSize     = 14.sp,
            lineHeight   = 20.sp,
            color        = tc.textPrimary
        ),
        bodyMedium = TextStyle(
            fontWeight   = FontWeight.Normal,
            fontSize     = 13.sp,
            lineHeight   = 18.sp,
            color        = tc.textPrimary
        ),
        bodySmall = TextStyle(
            fontWeight   = FontWeight.Normal,
            fontSize     = 12.sp,
            lineHeight   = 16.sp,
            color        = tc.textSecondary
        ),
        labelLarge = TextStyle(
            fontWeight   = FontWeight.SemiBold,
            fontSize     = 13.sp,
            color        = tc.textPrimary
        ),
        labelSmall = TextStyle(
            fontWeight   = FontWeight.Medium,
            fontSize     = 11.sp,
            color        = tc.textSecondary
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SceneFinderTheme
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SceneFinderTheme(
    theme: AppTheme = AppTheme.NEO_BRUTALISM,
    content: @Composable () -> Unit
) {
    val themeColors = getThemeColors(theme)
    CompositionLocalProvider(LocalAppThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = buildMaterialColorScheme(themeColors),
            typography  = buildTypography(themeColors),
            content     = content
        )
    }
}


