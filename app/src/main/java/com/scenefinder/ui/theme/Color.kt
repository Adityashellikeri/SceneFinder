package com.scenefinder.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Palette (Classic sleek Instagram/modern dark)
val IGBlack = Color(0xFF000000)
val IGDark = Color(0xFF0A0A0A)
val IGSurface = Color(0xFF121212)
val IGCard = Color(0xFF1C1C1E)
val IGCardElevated = Color(0xFF26262A)
val IGDivider = Color(0xFF2C2C2E)

val IGTextPrimary = Color(0xFFFFFFFF)
val IGTextSecondary = Color(0xFF8E8E8E)
val IGTextTertiary = Color(0xFF555555)

// Light Palette (Clean modern iOS/Material light)
val IGLightBg = Color(0xFFF8F9FA)
val IGLightSurface = Color(0xFFFFFFFF)
val IGLightCard = Color(0xFFF1F3F5)
val IGLightCardElevated = Color(0xFFE5E7EB)
val IGLightDivider = Color(0xFFE5E7EB)

val IGLightTextPrimary = Color(0xFF111827)
val IGLightTextSecondary = Color(0xFF6B7280)
val IGLightTextTertiary = Color(0xFF9CA3AF)

// Instagram gradient stop colours
val IGGradStart = Color(0xFFF58529)   // orange
val IGGradMid = Color(0xFFDD2A7B)     // pink
val IGGradEnd = Color(0xFF8134AF)     // purple

// Accent
val IGBlue = Color(0xFF0095F6)        // IG action blue
val IGStoryRing = Color(0xFFDD2A7B)
val IGVerified = Color(0xFF0095F6)
val IGRed = Color(0xFFED4956)
val IGGreen = Color(0xFF0DC143)
val IGYellow = Color(0xFFFFC300)

// Legacy aliases so existing code compiles unchanged
val DarkBackground = IGDark
val DarkSurface = IGSurface
val DarkSurfaceVariant = IGCard
val NeonAccent = IGBlue
val NeonGreen = IGGreen
val NeonRed = IGRed
val NeonOrange = IGGradStart
val TextPrimary = IGTextPrimary
val TextSecondary = IGTextSecondary
