package com.scenefinder.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import com.scenefinder.data.prefs.AppTheme
import android.media.ToneGenerator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scenefinder.ui.components.BrutalBadge
import com.scenefinder.ui.components.BrutalCard
import com.scenefinder.ui.components.brutalShadow
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// Universal Theme Searching Indicator – Movie & Game HUD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemeSearchingIndicator(
    query: String,
    modifier: Modifier = Modifier
) {
    val tc = LocalAppThemeColors.current
    when {
        tc.isCyberpunk -> CyberpunkNetrunnerSearching(query = query, modifier = modifier)
        tc.isIronMan   -> JarvisArcReactorSearching(query = query, modifier = modifier)
        tc.isBrutalist -> BrutalHazardSearching(query = query, modifier = modifier)
        else           -> MinimalOrbitalSearching(query = query, modifier = modifier)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CYBERPUNK 2077 HUD ANIMATIONS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Cyberpunk 2077 CRT phosphor scanlines & glowing holographic laser sweep.
 */
@Composable
fun CyberpunkScanlines(
    modifier: Modifier = Modifier,
    scanlineColor: Color = CyberpunkCyan.copy(alpha = 0.08f),
    sweepColor: Color = CyberpunkYellow.copy(alpha = 0.12f),
    lineSpacingDp: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_scan")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_progress"
    )
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = lineSpacingDp.toPx()
        val numLines = (size.height / spacingPx).toInt()

        // Draw horizontal CRT scanlines
        for (i in 0..numLines) {
            val y = i * spacingPx
            drawLine(
                color = scanlineColor.copy(alpha = scanlineColor.alpha * flickerAlpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // Draw glowing cathode laser sweep beam traveling downward
        val sweepY = size.height * sweepProgress
        val beamHeight = 48.dp.toPx()
        val sweepBrush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.5f to sweepColor.copy(alpha = sweepColor.alpha * flickerAlpha),
            1f to Color.Transparent,
            startY = sweepY - beamHeight / 2,
            endY = sweepY + beamHeight / 2
        )
        drawRect(
            brush = sweepBrush,
            topLeft = Offset(0f, sweepY - beamHeight / 2),
            size = Size(size.width, beamHeight)
        )
    }
}

/**
 * Cyberpunk 2077 Netrunner Breach Protocol Searching Indicator:
 * - Glitching decryptor title
 * - Rotating cyber reticle with angle notches
 * - Live scrolling hex data stream (e.g., 0x1C 0x55 0xBD 0xFA)
 * - Frequency waveform audio equalizer bars
 */
@Composable
fun CyberpunkNetrunnerSearching(
    query: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cp_search")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rot"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Live randomized hex stream
    var hexStream by remember { mutableStateOf("0x1C  0x55  0xBD  0xFA  0x77") }
    LaunchedEffect(Unit) {
        val hexChars = listOf("1C", "55", "BD", "E9", "7A", "FF", "3B", "D4", "0A", "88", "C1", "9E")
        while (true) {
            delay(180)
            hexStream = (0..4).joinToString("  ") { "0x" + hexChars[Random.nextInt(hexChars.size)] }
        }
    }

    BrutalCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        backgroundColor = CyberpunkCard,
        borderColor = CyberpunkYellow,
        shadowColor = CyberpunkRed.copy(alpha = 0.7f),
        shadowOffset = 5.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle scanlines across the card
            CyberpunkScanlines(
                modifier = Modifier.matchParentSize(),
                scanlineColor = CyberpunkYellow.copy(alpha = 0.05f),
                sweepColor = CyberpunkCyan.copy(alpha = 0.08f)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top HUD telemetry label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BREACH PROTOCOL // v2.077",
                        color = CyberpunkYellow,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "SIGNAL: LOCK 99.8%",
                        color = CyberpunkCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rotating Netrunner Cyber Reticle
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(90.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val r = size.minDimension / 2 - 4.dp.toPx()

                        // Outer dashed ring (rotating)
                        rotate(rotationAngle, center) {
                            drawCircle(
                                color = CyberpunkYellow.copy(alpha = 0.4f),
                                radius = r,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            // 4 cyber notches
                            for (i in 0..3) {
                                val rad = i * (PI / 2).toFloat()
                                val nx = center.x + r * cos(rad)
                                val ny = center.y + r * sin(rad)
                                drawCircle(
                                    color = CyberpunkRed,
                                    radius = 3.dp.toPx(),
                                    center = Offset(nx, ny)
                                )
                            }
                            // 2 neon cyber arc segments
                            drawArc(
                                color = CyberpunkCyan,
                                startAngle = 30f,
                                sweepAngle = 75f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawArc(
                                color = CyberpunkYellow,
                                startAngle = 210f,
                                sweepAngle = 75f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }

                        // Inner counter-rotating ring with tick marks
                        rotate(counterRotation, center) {
                            val innerR = r * 0.65f
                            drawCircle(
                                color = CyberpunkCyan.copy(alpha = 0.5f),
                                radius = innerR,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            for (j in 0..7) {
                                val rad = j * (PI / 4).toFloat()
                                val p1 = Offset(center.x + innerR * 0.85f * cos(rad), center.y + innerR * 0.85f * sin(rad))
                                val p2 = Offset(center.x + innerR * cos(rad), center.y + innerR * sin(rad))
                                drawLine(color = CyberpunkYellow, start = p1, end = p2, strokeWidth = 1.5.dp.toPx())
                            }
                        }

                        // Center core pulsing reticle dot
                        drawCircle(
                            color = CyberpunkYellow.copy(alpha = pulseGlow),
                            radius = 6.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = CyberpunkRed,
                            radius = 2.5.dp.toPx(),
                            center = center
                        )
                    }

                    // Netrunner icon in center
                    Text(
                        text = "0x77",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberpunkYellow.copy(alpha = pulseGlow)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrolling Live Hex Matrix Stream
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0D0D14))
                        .border(1.dp, CyberpunkCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = hexStream,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberpunkCyan,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Glitch deciphering query text
                GlitchDecipherText(
                    targetText = "DECRYPTING // \"$query\"",
                    textColor = CyberpunkYellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Audio / Telemetry Equalizer Spectrum Bars
                CyberAudioSpectrumBars(
                    barCount = 18,
                    barColor = CyberpunkCyan,
                    accentColor = CyberpunkYellow,
                    modifier = Modifier.height(18.dp)
                )
            }
        }
    }
}

/**
 * Animated audio spectrum equalizer bars.
 */
@Composable
fun CyberAudioSpectrumBars(
    barCount: Int = 16,
    barColor: Color = CyberpunkCyan,
    accentColor: Color = CyberpunkYellow,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxWidth()) {
        val barWidth = 4.dp.toPx()
        val totalBarsWidth = barCount * barWidth
        val availableSpacing = (size.width - totalBarsWidth) / (barCount - 1).coerceAtLeast(1)
        val maxH = size.height

        for (i in 0 until barCount) {
            val wave = (sin(animOffset + i * 0.5f) + 1f) / 2f
            val wave2 = (cos(animOffset * 1.5f + i * 0.7f) + 1f) / 2f
            val combined = (wave * 0.6f + wave2 * 0.4f).coerceIn(0.15f, 1f)
            val barH = maxH * combined
            val x = i * (barWidth + availableSpacing)
            val y = maxH - barH

            val color = if (i % 4 == 0) accentColor else barColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IRON MAN / STARK INDUSTRIES JARVIS HUD ANIMATIONS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Iron Man Mark-85 JARVIS Arc Reactor Searching Indicator:
 * - Rotating holographic concentric Arc Reactor rings
 * - Sweeping 360° radar laser
 * - Telemetry targeting readout ("TARGET ACQUISITION: ACTIVE", "STARK SATELLITE UPLINK")
 * - Oscillating sine waveform
 */
@Composable
fun JarvisArcReactorSearching(
    query: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_search")

    val reactorRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reactor_rot"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reactor_counter_rot"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rot"
    )

    BrutalCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        backgroundColor = IronManCard,
        borderColor = IronManGold,
        shadowColor = IronManRed.copy(alpha = 0.7f),
        shadowOffset = 5.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Holographic sweep beam
            JarvisHoloSweep(modifier = Modifier.matchParentSize())

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Stark Industries Top Telemetry Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STARK IND. // MARK-85 HUD",
                        color = IronManGold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "FRIDAY: TGT_LOCK 99%",
                        color = IronManBlueBright,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // The Iconic Spinning Arc Reactor Core
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxR = size.minDimension / 2 - 4.dp.toPx()

                        // Outer telemetry ring with 4 target crosshairs
                        drawCircle(
                            color = IronManGold.copy(alpha = 0.35f),
                            radius = maxR,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        // 4 cardinal target brackets
                        val bracketLen = 8.dp.toPx()
                        drawLine(color = IronManGold, start = Offset(center.x - maxR, center.y), end = Offset(center.x - maxR + bracketLen, center.y), strokeWidth = 2.dp.toPx())
                        drawLine(color = IronManGold, start = Offset(center.x + maxR - bracketLen, center.y), end = Offset(center.x + maxR, center.y), strokeWidth = 2.dp.toPx())
                        drawLine(color = IronManGold, start = Offset(center.x, center.y - maxR), end = Offset(center.x, center.y - maxR + bracketLen), strokeWidth = 2.dp.toPx())
                        drawLine(color = IronManGold, start = Offset(center.x, center.y + maxR - bracketLen), end = Offset(center.x, center.y + maxR), strokeWidth = 2.dp.toPx())

                        // Mid ring: 3 rotating Arc segments (Clockwise)
                        rotate(reactorRotation, center) {
                            val midR = maxR * 0.78f
                            // Draw 3 arc sections
                            for (k in 0..2) {
                                val startAng = k * 120f + 15f
                                drawArc(
                                    color = IronManBlueBright,
                                    startAngle = startAng,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }

                        // Inner telemetry ring: 12 tick teeth (Counter-Clockwise)
                        rotate(counterRotation, center) {
                            val innerR = maxR * 0.55f
                            drawCircle(
                                color = IronManGoldBright.copy(alpha = 0.5f),
                                radius = innerR,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            for (t in 0..11) {
                                val rad = t * (PI / 6).toFloat()
                                val t1 = Offset(center.x + innerR * 0.8f * cos(rad), center.y + innerR * 0.8f * sin(rad))
                                val t2 = Offset(center.x + innerR * cos(rad), center.y + innerR * sin(rad))
                                drawLine(color = IronManGold, start = t1, end = t2, strokeWidth = 1.5.dp.toPx())
                            }
                        }

                        // 360° Radar sweep line
                        rotate(radarAngle, center) {
                            drawLine(
                                color = IronManBlueBright.copy(alpha = 0.8f),
                                start = center,
                                end = Offset(center.x, center.y - maxR * 0.9f),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // Central Unibeam core: pulsing arc glow
                        val coreR = 10.dp.toPx() * corePulse
                        drawCircle(
                            color = IronManBlueBright.copy(alpha = 0.35f),
                            radius = coreR * 1.6f,
                            center = center
                        )
                        drawCircle(
                            color = IronManBlueBright,
                            radius = coreR,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = coreR * 0.5f,
                            center = center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Telemetry Coordinate readout
                Text(
                    text = "TARGET ACQUISITION: IN PROGRESS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = IronManGoldBright,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "\"$query\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Diagnostic Telemetry Waveform (oscillating sine wave)
                JarvisTelemetryWaveform(
                    waveColor = IronManBlueBright,
                    gridColor = IronManGold.copy(alpha = 0.25f),
                    modifier = Modifier.height(24.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SATELLITE FREQ: 433.92 MHz // ENCRYPTED LINK",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = IronManBlueBright.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Stark holographic laser scan beam sweeping vertically.
 */
@Composable
fun JarvisHoloSweep(
    modifier: Modifier = Modifier,
    laserColor: Color = IronManBlueBright.copy(alpha = 0.15f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_sweep")
    val sweepY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_y"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val y = size.height * sweepY
        val beamHeight = 40.dp.toPx()
        val sweepBrush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.5f to laserColor,
            1f to Color.Transparent,
            startY = y - beamHeight / 2,
            endY = y + beamHeight / 2
        )
        drawRect(
            brush = sweepBrush,
            topLeft = Offset(0f, y - beamHeight / 2),
            size = Size(size.width, beamHeight)
        )
        // High-intensity laser line
        drawLine(
            color = IronManBlueBright.copy(alpha = 0.4f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

/**
 * Holographic sine wave telemetry graph.
 */
@Composable
fun JarvisTelemetryWaveform(
    waveColor: Color = IronManBlueBright,
    gridColor: Color = IronManGold.copy(alpha = 0.2f),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val midY = h / 2

        // Background grid lines
        drawLine(color = gridColor, start = Offset(0f, midY), end = Offset(w, midY), strokeWidth = 1f)
        for (gx in 0..4) {
            val x = gx * (w / 4)
            drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
        }

        // Draw animated sine telemetry wave
        val path = Path()
        val step = 4f
        var first = true

        var x = 0f
        while (x <= w) {
            val normX = x / w
            val y = midY + sin(normX * 4 * PI.toFloat() + phase) * (h * 0.4f) * sin(normX * PI.toFloat())
            if (first) {
                path.moveTo(x, y.toFloat())
                first = false
            } else {
                path.lineTo(x, y.toFloat())
            }
            x += step
        }

        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEO-BRUTALISM ANIMATIONS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Infinite scrolling diagonal yellow-and-black hazard caution tape.
 */
@Composable
fun BrutalHazardTape(
    modifier: Modifier = Modifier,
    tapeHeight: Dp = 22.dp,
    stripeColor: Color = BrutalYellow,
    backgroundColor: Color = BrutalBlack,
    text: String = "/// DETECTING MEDIA /// SCANNING ARCHIVES /// EXECUTING QUERY ///"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hazard_tape")
    val offsetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tape_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tapeHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(1.5.dp, BrutalBlack, RoundedCornerShape(4.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stripeWidth = 16.dp.toPx()
            val totalPeriod = stripeWidth * 2
            val shift = offsetProgress * totalPeriod

            var startX = -totalPeriod + shift
            while (startX < size.width + totalPeriod) {
                val path = Path().apply {
                    moveTo(startX, size.height)
                    lineTo(startX + stripeWidth * 0.8f, 0f)
                    lineTo(startX + stripeWidth * 1.8f, 0f)
                    lineTo(startX + stripeWidth, size.height)
                    close()
                }
                drawPath(path = path, color = stripeColor)
                startX += totalPeriod
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = BrutalBlack,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

/**
 * Neo-Brutalist Mechanical Stamping Search Indicator:
 * - Animated caution hazard bar
 * - Electric pulsing bolt with punchy bold typewriter
 */
@Composable
fun BrutalHazardSearching(
    query: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "brutal_search")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    BrutalCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        backgroundColor = BrutalCard,
        borderColor = BrutalCyan,
        shadowColor = BrutalBlack,
        shadowOffset = 5.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Scrolling Hazard Caution Bar
            BrutalHazardTape(
                tapeHeight = 22.dp,
                stripeColor = BrutalYellow,
                backgroundColor = BrutalBlack,
                text = "/// SEARCHING ALL REPOSITORIES ///"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Punchy Electric Bolt with Hydraulic Pulse
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrutalYellow)
                    .border(2.5.dp, BrutalBlack, RoundedCornerShape(12.dp))
                    .brutalShadow(offset = 3.dp, color = BrutalBlack, cornerRadius = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚡",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            BrutalBadge(
                text = "EXECUTING QUERY",
                backgroundColor = BrutalCyan,
                textColor = BrutalBlack,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"$query\"",
                color = BrutalTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Second bottom hazard bar
            BrutalHazardTape(
                tapeHeight = 18.dp,
                stripeColor = BrutalPink,
                backgroundColor = BrutalBlack,
                text = "/// ANILIST • TMDB • MANGADEX ///"
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DARK MINIMAL ANIMATIONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MinimalOrbitalSearching(
    query: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "minimal_search")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF121212))
            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(14.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Elegant orbital ring with orbiting satellite dot
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(54.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val r = size.minDimension / 2 - 4.dp.toPx()

                    drawCircle(
                        color = Color(0xFF6C63FF).copy(alpha = 0.3f),
                        radius = r,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    rotate(orbitAngle, center) {
                        drawCircle(
                            color = Color(0xFF6C63FF).copy(alpha = glowAlpha),
                            radius = 4.dp.toPx(),
                            center = Offset(center.x, center.y - r)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Searching archives...",
                color = Color(0xFFEEEEEE),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"$query\"",
                color = Color(0xFF888888),
                fontSize = 13.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AMBIENT THEME OVERLAY (Movie/Game HUD layer)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Ambient overlay applied to cards or screens that injects cinematic atmosphere:
 * - Cyberpunk: Scanlines, cathode CRT sweep, tech corner notches, hex matrix background
 * - Iron Man: Holographic Arc Reactor watermark, blueprint grid, corner brackets & laser scan line
 * - Brutalism / Dark Minimal: Subtle crisp accents
 */
@Composable
fun ThemeAmbientOverlay(
    modifier: Modifier = Modifier
) {
    val tc = LocalAppThemeColors.current
    when {
        tc.isCyberpunk -> {
            Box(modifier = modifier.fillMaxSize()) {
                // Background cybernetic grid & floating hex stream
                CyberpunkBackgroundGrid(modifier = Modifier.matchParentSize())
                CyberpunkBackgroundHexStream(modifier = Modifier.matchParentSize())
                // Phosphor scanlines
                CyberpunkScanlines(
                    modifier = Modifier.matchParentSize(),
                    scanlineColor = CyberpunkCyan.copy(alpha = 0.05f),
                    sweepColor = CyberpunkYellow.copy(alpha = 0.08f)
                )
                CyberCornerTechNotches(
                    modifier = Modifier.matchParentSize(),
                    color = CyberpunkYellow.copy(alpha = 0.7f)
                )
            }
        }
        tc.isIronMan -> {
            Box(modifier = modifier.fillMaxSize()) {
                // Holographic HUD blueprint grid
                JarvisBackgroundGrid(modifier = Modifier.matchParentSize())
                // Rotating holographic Arc Reactor watermark
                JarvisBackgroundArcReactorWatermark(modifier = Modifier.matchParentSize())
                // Sweeping laser scan beam
                JarvisHoloSweep(
                    modifier = Modifier.matchParentSize(),
                    laserColor = IronManBlueBright.copy(alpha = 0.10f)
                )
                JarvisCornerTechBrackets(
                    modifier = Modifier.matchParentSize(),
                    color = IronManGold.copy(alpha = 0.7f),
                    cyanColor = IronManBlueBright.copy(alpha = 0.6f)
                )
            }
        }
        tc.isBrutalist -> {
            Box(modifier = modifier.fillMaxSize()) {
                BrutalBackgroundGrid(modifier = Modifier.matchParentSize())
            }
        }
        else -> {
            // Dark Minimal subtle radial breathing glow
            MinimalBreathingBackground(modifier = modifier)
        }
    }
}

/**
 * Cyberpunk Corner Tech Notches [ ┌ ┐ └ ┘ ]
 */
@Composable
fun CyberCornerTechNotches(
    modifier: Modifier = Modifier,
    color: Color = CyberpunkYellow
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val len = 14.dp.toPx()
        val stroke = 2.dp.toPx()
        val pad = 6.dp.toPx()

        // Top-Left
        drawLine(color = color, start = Offset(pad, pad), end = Offset(pad + len, pad), strokeWidth = stroke)
        drawLine(color = color, start = Offset(pad, pad), end = Offset(pad, pad + len), strokeWidth = stroke)

        // Top-Right
        drawLine(color = color, start = Offset(size.width - pad, pad), end = Offset(size.width - pad - len, pad), strokeWidth = stroke)
        drawLine(color = color, start = Offset(size.width - pad, pad), end = Offset(size.width - pad, pad + len), strokeWidth = stroke)

        // Bottom-Left
        drawLine(color = color, start = Offset(pad, size.height - pad), end = Offset(pad + len, size.height - pad), strokeWidth = stroke)
        drawLine(color = color, start = Offset(pad, size.height - pad), end = Offset(pad, size.height - pad - len), strokeWidth = stroke)

        // Bottom-Right
        drawLine(color = color, start = Offset(size.width - pad, size.height - pad), end = Offset(size.width - pad - len, size.height - pad), strokeWidth = stroke)
        drawLine(color = color, start = Offset(size.width - pad, size.height - pad), end = Offset(size.width - pad, size.height - pad - len), strokeWidth = stroke)
    }
}

/**
 * Iron Man HUD Corner Tech Brackets with Stark targeting crosshairs
 */
@Composable
fun JarvisCornerTechBrackets(
    modifier: Modifier = Modifier,
    color: Color = IronManGold,
    cyanColor: Color = IronManBlueBright
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val len = 16.dp.toPx()
        val stroke = 2.dp.toPx()
        val pad = 8.dp.toPx()

        // Top-Left
        drawLine(color = color, start = Offset(pad, pad), end = Offset(pad + len, pad), strokeWidth = stroke)
        drawLine(color = color, start = Offset(pad, pad), end = Offset(pad, pad + len), strokeWidth = stroke)
        drawCircle(color = cyanColor, radius = 2.dp.toPx(), center = Offset(pad + len, pad))

        // Top-Right
        drawLine(color = color, start = Offset(size.width - pad, pad), end = Offset(size.width - pad - len, pad), strokeWidth = stroke)
        drawLine(color = color, start = Offset(size.width - pad, pad), end = Offset(size.width - pad, pad + len), strokeWidth = stroke)
        drawCircle(color = cyanColor, radius = 2.dp.toPx(), center = Offset(size.width - pad - len, pad))

        // Bottom-Left
        drawLine(color = color, start = Offset(pad, size.height - pad), end = Offset(pad + len, size.height - pad), strokeWidth = stroke)
        drawLine(color = color, start = Offset(pad, size.height - pad), end = Offset(pad, size.height - pad - len), strokeWidth = stroke)

        // Bottom-Right
        drawLine(color = color, start = Offset(size.width - pad, size.height - pad), end = Offset(size.width - pad - len, size.height - pad), strokeWidth = stroke)
        drawLine(color = color, start = Offset(size.width - pad, size.height - pad), end = Offset(size.width - pad, size.height - pad - len), strokeWidth = stroke)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GLITCH & SCI-FI DECIPHERING TEXT
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sci-Fi deciphering text animation (like Cyberpunk hacking or Stark decryption):
 * Characters rapidly cycle through random hex and tech runes before locking into place.
 */
@Composable
fun GlitchDecipherText(
    targetText: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    var displayedText by remember(targetText) { mutableStateOf(targetText) }

    LaunchedEffect(targetText) {
        val runes = "01#%&*!?>_7XFA"
        val length = targetText.length
        val iterations = 8

        for (step in 0..iterations) {
            val lockedCount = (step.toFloat() / iterations * length).toInt()
            val sb = java.lang.StringBuilder()
            for (i in 0 until length) {
                if (i < lockedCount) {
                    sb.append(targetText[i])
                } else if (targetText[i] == ' ' || targetText[i] == '"' || targetText[i] == '/') {
                    sb.append(targetText[i])
                } else {
                    sb.append(runes[Random.nextInt(runes.length)])
                }
            }
            displayedText = sb.toString()
            delay(55)
        }
        displayedText = targetText
    }

    Text(
        text = displayedText,
        color = textColor,
        fontFamily = fontFamily,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// IMMERSIVE THEME BACKGROUND WATERMARKS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Large rotating Arc Reactor holographic watermark in the background for Iron Man theme.
 */
@Composable
fun JarvisBackgroundArcReactorWatermark(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_bg_arc")
    val rotClockwise by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_rot_cw"
    )
    val rotCounter by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_rot_ccw"
    )
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.82f, size.height * 0.35f)
        val maxR = size.minDimension * 0.65f

        // Outer concentric HUD circle
        drawCircle(
            color = IronManGold.copy(alpha = 0.09f),
            radius = maxR,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Clockwise segmented arcs
        rotate(rotClockwise, center) {
            val arcR = maxR * 0.82f
            for (i in 0..2) {
                drawArc(
                    color = IronManBlueBright.copy(alpha = 0.12f),
                    startAngle = i * 120f + 10f,
                    sweepAngle = 85f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcR, center.y - arcR),
                    size = Size(arcR * 2, arcR * 2),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Counter-clockwise gear tooth telemetry ring
        rotate(rotCounter, center) {
            val innerR = maxR * 0.58f
            drawCircle(
                color = IronManGold.copy(alpha = 0.08f),
                radius = innerR,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            for (t in 0..11) {
                val rad = t * (PI / 6).toFloat()
                val p1 = Offset(center.x + innerR * 0.85f * cos(rad), center.y + innerR * 0.85f * sin(rad))
                val p2 = Offset(center.x + innerR * cos(rad), center.y + innerR * sin(rad))
                drawLine(
                    color = IronManGold.copy(alpha = 0.12f),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // Central glowing Unibeam core
        val coreR = 24.dp.toPx() * corePulse
        drawCircle(
            color = IronManBlueBright.copy(alpha = 0.08f),
            radius = coreR * 2.2f,
            center = center
        )
        drawCircle(
            color = IronManBlueBright.copy(alpha = 0.18f),
            radius = coreR,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = coreR * 0.45f,
            center = center
        )
    }
}

/**
 * Stark HUD blueprint grid lines and coordinate readouts.
 */
@Composable
fun JarvisBackgroundGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridColor = IronManGold.copy(alpha = 0.04f)
        val step = 48.dp.toPx()

        var x = 0f
        while (x < size.width) {
            drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
    }
}

/**
 * Cyberpunk background grid lines.
 */
@Composable
fun CyberpunkBackgroundGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridColor = CyberpunkCyan.copy(alpha = 0.04f)
        val step = 44.dp.toPx()

        var x = 0f
        while (x < size.width) {
            drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
    }
}

/**
 * Faint drifting Netrunner hex code stream in background for Cyberpunk theme.
 */
@Composable
fun CyberpunkBackgroundHexStream(modifier: Modifier = Modifier) {
    var hexLines by remember {
        mutableStateOf(
            listOf(
                "0x1C  0x55  0xBD  0xE9  0x7A  0xFF",
                "0x3B  0xD4  0x0A  0x88  0xC1  0x9E",
                "0x77  0x01  0xFA  0x2E  0x6D  0x11",
                "0x88  0xB2  0x04  0xF9  0xAA  0x55"
            )
        )
    }

    LaunchedEffect(Unit) {
        val hexPool = listOf("1C", "55", "BD", "E9", "7A", "FF", "3B", "D4", "0A", "88", "C1", "9E", "77", "42")
        while (true) {
            delay(1200)
            hexLines = (0..3).map {
                (0..5).joinToString("  ") { "0x" + hexPool[Random.nextInt(hexPool.size)] }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, top = 80.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        hexLines.forEach { line ->
            Text(
                text = line,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberpunkCyan.copy(alpha = 0.06f),
                letterSpacing = 2.sp
            )
        }
    }
}

/**
 * Neo-Brutalist geometric dot grid background.
 */
@Composable
fun BrutalBackgroundGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val dotColor = BrutalYellow.copy(alpha = 0.06f)
        val step = 32.dp.toPx()
        val radius = 1.5.dp.toPx()

        var x = step / 2
        while (x < size.width) {
            var y = step / 2
            while (y < size.height) {
                drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                y += step
            }
            x += step
        }
    }
}

/**
 * Dark Minimal ambient breathing gradient.
 */
@Composable
fun MinimalBreathingBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "minimal_bg")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "min_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF6C63FF).copy(alpha = glowAlpha),
                Color.Transparent
            ),
            center = Offset(size.width / 2, size.height * 0.4f),
            radius = size.minDimension * 0.8f
        )
        drawRect(brush = brush)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// THEME STARTUP INTRO (App Opening Arc Reactor / Cyberpunk Boot Sequence)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemeStartupIntro(
    theme: AppTheme,
    onFinished: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            ),
        contentAlignment = Alignment.Center
    ) {
        when (theme) {
            AppTheme.IRON_MAN -> IronManStartupSequence(onFinished = onFinished)
            AppTheme.CYBERPUNK_2077 -> CyberpunkStartupSequence(onFinished = onFinished)
            AppTheme.NEO_BRUTALISM -> BrutalStartupSequence(onFinished = onFinished)
            AppTheme.DARK_MINIMAL -> LaunchedEffect(Unit) { onFinished() }
        }
    }
}

/**
 * Iron Man Arc Reactor App Opening Startup Sequence.
 */
@Composable
private fun IronManStartupSequence(onFinished: () -> Unit) {
    val context = LocalContext.current
    var telemetryStage by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        SciFiSoundFx.playTone(ToneGenerator.TONE_PROP_PROMPT, 200)
        SciFiSoundFx.vibrate(context, longArrayOf(0, 80, 40, 120))
        delay(600)
        telemetryStage = 1
        delay(700)
        telemetryStage = 2
        delay(900)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "arc_startup")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "startup_rot"
    )
    val coreGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "startup_glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        // Holographic Arc Reactor
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxR = size.minDimension / 2 - 8.dp.toPx()

                // Outer targeting telemetry ring
                drawCircle(
                    color = IronManGold,
                    radius = maxR,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 4 cardinal target brackets
                val bLen = 14.dp.toPx()
                drawLine(color = IronManGold, start = Offset(center.x - maxR, center.y), end = Offset(center.x - maxR + bLen, center.y), strokeWidth = 3.dp.toPx())
                drawLine(color = IronManGold, start = Offset(center.x + maxR - bLen, center.y), end = Offset(center.x + maxR, center.y), strokeWidth = 3.dp.toPx())
                drawLine(color = IronManGold, start = Offset(center.x, center.y - maxR), end = Offset(center.x, center.y - maxR + bLen), strokeWidth = 3.dp.toPx())
                drawLine(color = IronManGold, start = Offset(center.x, center.y + maxR - bLen), end = Offset(center.x, center.y + maxR), strokeWidth = 3.dp.toPx())

                // Rotating segmented Arc segments
                rotate(rotationAngle, center) {
                    val midR = maxR * 0.78f
                    for (k in 0..2) {
                        drawArc(
                            color = IronManBlueBright,
                            startAngle = k * 120f + 10f,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                }

                // Inner gear teeth counter-rotating
                rotate(-rotationAngle * 1.5f, center) {
                    val innerR = maxR * 0.54f
                    drawCircle(color = IronManGoldBright, radius = innerR, style = Stroke(width = 1.5.dp.toPx()))
                    for (t in 0..11) {
                        val rad = t * (PI / 6).toFloat()
                        val t1 = Offset(center.x + innerR * 0.8f * cos(rad), center.y + innerR * 0.8f * sin(rad))
                        val t2 = Offset(center.x + innerR * cos(rad), center.y + innerR * sin(rad))
                        drawLine(color = IronManGoldBright, start = t1, end = t2, strokeWidth = 2.dp.toPx())
                    }
                }

                // Central pulsing Unibeam core
                val coreR = 18.dp.toPx() * coreGlow
                drawCircle(
                    color = IronManBlueBright.copy(alpha = 0.35f),
                    radius = coreR * 1.8f,
                    center = center
                )
                drawCircle(
                    color = IronManBlueBright,
                    radius = coreR,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = coreR * 0.5f,
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "STARK INDUSTRIES // MARK-85",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = IronManGold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when (telemetryStage) {
                0 -> "INITIALIZING ARC REACTOR..."
                1 -> "ARC CORE: 100% ONLINE • SYSTEMS NOMINAL"
                else -> "“WELCOME BACK, SIR. READY FOR ACTION.”"
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = IronManBlueBright,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "TAP TO SKIP",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666677),
            letterSpacing = 1.sp
        )
    }
}

/**
 * Cyberpunk 2077 Kiroshi Optics Boot Sequence.
 */
@Composable
private fun CyberpunkStartupSequence(onFinished: () -> Unit) {
    val context = LocalContext.current
    var bootStage by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        SciFiSoundFx.playTone(ToneGenerator.TONE_CDMA_HIGH_L, 160)
        SciFiSoundFx.vibrate(context, longArrayOf(0, 50, 50, 100))
        delay(600)
        bootStage = 1
        delay(700)
        bootStage = 2
        delay(900)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CyberpunkScanlines(modifier = Modifier.matchParentSize())
        CyberCornerTechNotches(modifier = Modifier.matchParentSize(), color = CyberpunkYellow)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "0x77", fontFamily = FontFamily.Monospace, fontSize = 48.sp, fontWeight = FontWeight.Black, color = CyberpunkYellow)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "KIROSHI OPTICS // CYBERWARE v2.077",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = CyberpunkCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (bootStage) {
                    0 -> "NEURAL LINK CONNECTING..."
                    1 -> "OPTIC SYNC: COMPLETE • LATENCY 0.2ms"
                    else -> "“WAKE UP, SAMURAI. WE HAVE A CITY TO BURN.”"
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (bootStage == 2) CyberpunkRed else CyberpunkYellow,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(text = "TAP TO SKIP", fontSize = 10.sp, color = Color(0xFF666677))
        }
    }
}

/**
 * Neo-Brutalism Kinetic Slam Startup.
 */
@Composable
private fun BrutalStartupSequence(onFinished: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SciFiSoundFx.playTone(ToneGenerator.TONE_PROP_PROMPT, 120)
        SciFiSoundFx.vibrate(context, longArrayOf(0, 70))
        delay(1600)
        onFinished()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        BrutalBadge(text = "⚡ SCENEFINDER", backgroundColor = BrutalYellow, textColor = Color.Black, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(14.dp))
        BrutalHazardTape(tapeHeight = 22.dp, stripeColor = BrutalCyan, backgroundColor = BrutalBlack, text = "/// MAXIMUM OVERDRIVE /// BOOTING SYSTEM ///")
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = "TAP TO SKIP", fontSize = 10.sp, color = Color(0xFF666677))
    }
}

