package com.scenefinder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val IGColorScheme = darkColorScheme(
    primary = IGBlue,
    onPrimary = IGTextPrimary,
    primaryContainer = IGCard,
    onPrimaryContainer = IGTextPrimary,
    secondary = IGGradMid,
    onSecondary = IGTextPrimary,
    error = IGRed,
    onError = IGTextPrimary,
    background = IGBlack,
    onBackground = IGTextPrimary,
    surface = IGSurface,
    onSurface = IGTextPrimary,
    surfaceVariant = IGCard,
    onSurfaceVariant = IGTextSecondary,
    outline = IGDivider,
    outlineVariant = IGTextTertiary
)

private val IGTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        color = IGTextPrimary
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = IGTextPrimary
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = IGTextPrimary
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = IGTextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = IGTextPrimary
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = IGTextPrimary
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = IGTextPrimary
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = IGTextSecondary
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = IGTextPrimary
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = IGTextSecondary
    )
)

@Composable
fun SceneFinderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IGColorScheme,
        typography = IGTypography,
        content = content
    )
}
