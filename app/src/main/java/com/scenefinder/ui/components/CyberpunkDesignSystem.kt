package com.scenefinder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scenefinder.ui.theme.*
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Cyberpunk Design System — shared tokens & composable helpers
// ─────────────────────────────────────────────────────────────────────────────

object CyberColors {
    val cyan        = NeonCyan
    val magenta     = NeonMagenta
    val violet      = NeonViolet
    val lime        = NeonGreenLime
    val toxicYellow = NeonYellowGreen
    val pinkSalmon  = NeonOrangePink
    val bg          = CyberBlack
    val surface     = CyberSurface
    val card        = CyberCard
    val textPri     = CyberTextPrimary
    val textSec     = CyberTextSecondary

    fun heroGradient() = Brush.horizontalGradient(listOf(cyan, violet, magenta))
    fun cardBorder(accentColor: Color = cyan) = accentColor.copy(alpha = 0.65f)
    fun glowBrush(color: Color = cyan) = Brush.radialGradient(
        colors = listOf(color.copy(alpha = 0.35f), Color.Transparent),
        radius = 420f
    )
}

// ─── Animated neon grid background ───────────────────────────────────────────
@Composable
fun CyberpunkGridBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid_scroll")
    val scrollY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_scroll_y"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "grid_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSpacing = 64.dp.toPx()
        val lineColor = NeonCyan.copy(alpha = pulseAlpha)
        val lineColorMag = NeonMagenta.copy(alpha = pulseAlpha * 0.5f)
        val paint = Paint().apply { color = lineColor }
        val offset = scrollY % gridSpacing

        // Horizontal lines (scrolling downward slowly)
        var y = -gridSpacing + offset
        while (y < size.height + gridSpacing) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.7f
            )
            y += gridSpacing
        }

        // Vertical lines
        var x = 0f
        while (x < size.width + gridSpacing) {
            drawLine(
                color = lineColorMag,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.7f
            )
            x += gridSpacing
        }

        // Perspective horizon glow at center
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, NeonCyan.copy(alpha = 0.35f), NeonMagenta.copy(alpha = 0.25f), Color.Transparent)
            ),
            start = Offset(0f, size.height * 0.55f),
            end = Offset(size.width, size.height * 0.55f),
            strokeWidth = 2f
        )
    }
}

// ─── Neon border modifier ─────────────────────────────────────────────────────
fun Modifier.neonBorder(
    color: Color = NeonCyan,
    cornerRadius: Dp = 16.dp,
    width: Dp = 1.dp
): Modifier = this.border(
    width = width,
    brush = Brush.linearGradient(
        colors = listOf(
            color.copy(alpha = 0.9f),
            color.copy(alpha = 0.3f),
            NeonMagenta.copy(alpha = 0.5f),
            color.copy(alpha = 0.8f)
        )
    ),
    shape = RoundedCornerShape(cornerRadius)
)

// ─── Neon glow box shadow effect ─────────────────────────────────────────────
fun Modifier.neonGlow(
    color: Color = NeonCyan,
    glowRadius: Dp = 24.dp
): Modifier = this.drawBehind {
    val radiusPx = glowRadius.toPx()
    val paint = Paint()
    paint.asFrameworkPaint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.TRANSPARENT
        setShadowLayer(radiusPx, 0f, 0f, color.copy(alpha = 0.8f).toArgb())
    }
    drawContext.canvas.nativeCanvas.drawRoundRect(
        android.graphics.RectF(0f, 0f, size.width, size.height),
        glowRadius.toPx() / 2,
        glowRadius.toPx() / 2,
        paint.asFrameworkPaint()
    )
}

// ─── Glitch text composable ───────────────────────────────────────────────────
@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    fontWeight: FontWeight = FontWeight.Black,
    primaryColor: Color = NeonCyan,
    glitchColor1: Color = NeonMagenta,
    glitchColor2: Color = NeonViolet
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glitch")
    val glitchOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                0f at 3000      // 75% time: no glitch
                1f at 3100      // fast glitch burst
                0f at 3200
                1f at 3300
                0f at 3400
                0f at 4000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "glitch_offset"
    )

    val baseStyle = TextStyle(
        fontSize    = fontSize,
        fontWeight  = fontWeight,
        color       = primaryColor,
        letterSpacing = (-0.5).sp
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Ghost layer 1 (magenta, offset left)
        if (glitchOffset > 0.5f) {
            Text(
                text      = text,
                style     = baseStyle.copy(color = glitchColor1.copy(alpha = 0.7f)),
                modifier  = Modifier.offset(x = (-2).dp, y = 1.dp)
            )
        }
        // Ghost layer 2 (violet, offset right)
        if (glitchOffset > 0.5f) {
            Text(
                text      = text,
                style     = baseStyle.copy(color = glitchColor2.copy(alpha = 0.5f)),
                modifier  = Modifier.offset(x = 2.dp, y = (-1).dp)
            )
        }
        // Main text
        Text(
            text  = text,
            style = TextStyle(
                fontSize      = fontSize,
                fontWeight    = fontWeight,
                brush         = Brush.horizontalGradient(listOf(primaryColor, glitchColor1)),
                letterSpacing = (-0.5).sp
            )
        )
    }
}

// ─── Neon pulsing dot ─────────────────────────────────────────────────────────
@Composable
fun NeonPulsingDot(
    color: Color = NeonCyan,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neon_dot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.25f,
        animationSpec = infiniteRepeatable(
            animation    = tween(900, easing = FastOutSlowInEasing),
            repeatMode   = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation    = tween(900, easing = FastOutSlowInEasing),
            repeatMode   = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Glow ring
        Box(
            modifier = Modifier
                .size(size * scale)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = alpha * 0.3f))
        )
        // Core dot
        Box(
            modifier = Modifier
                .size(size * 0.55f)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = alpha))
        )
    }
}

// ─── Scanline overlay effect ─────────────────────────────────────────────────
@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.06f),
                start = Offset(0f, y),
                end   = Offset(size.width, y),
                strokeWidth = 1.2f
            )
            y += 3f
        }
    }
}

// ─── Cyber section header ─────────────────────────────────────────────────────
@Composable
fun CyberSectionLabel(
    text: String,
    accentColor: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Neon tick mark
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.2f)))
                )
        )
        Text(
            text         = text.uppercase(),
            fontSize     = 11.sp,
            fontWeight   = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color        = accentColor
        )
    }
}
