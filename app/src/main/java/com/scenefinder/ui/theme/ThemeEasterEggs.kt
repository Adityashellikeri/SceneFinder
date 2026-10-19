package com.scenefinder.ui.theme

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.scenefinder.ui.components.BrutalBadge
import com.scenefinder.ui.components.BrutalButton
import com.scenefinder.ui.components.BrutalCard
import com.scenefinder.ui.components.brutalShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// Easter Egg Types
// ─────────────────────────────────────────────────────────────────────────────

sealed class EasterEggType {
    object CyberpunkRelic : EasterEggType()
    object IronManProtocol : EasterEggType()
    object OnePieceGear5 : EasterEggType()
    object NeoBrutalistOverdrive : EasterEggType()

    companion object {
        fun detectFromQuery(query: String): EasterEggType? {
            val q = query.trim().lowercase()
            return when {
                q.contains("cyberpunk") || q.contains("samurai") || q.contains("silverhand") ||
                q.contains("edgerunner") || q.contains("sandevistan") || q.contains("arasaka") ||
                q.contains("night city") || q.contains("choom") || q.contains("relic") -> CyberpunkRelic

                q.contains("iron man") || q.contains("tony stark") || q.contains("stark") ||
                q.contains("jarvis") || q.contains("friday") || q.contains("arc reactor") ||
                q.contains("3000") || q.contains("veronica") || q.contains("unibeam") ||
                q.contains("i am iron man") -> IronManProtocol

                q.contains("one piece") || q.contains("gear 5") || q.contains("luffy") ||
                q.contains("joyboy") || q.contains("drums of liberation") || q.contains("zoro") -> OnePieceGear5

                q.contains("brutal") || q.contains("overdrive") || q.contains("konami") ||
                q.contains("matrix") || q.contains("hack") -> NeoBrutalistOverdrive

                else -> null
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Audio & Haptic Helper for Sci-Fi HUD effects
// ─────────────────────────────────────────────────────────────────────────────

object SciFiSoundFx {
    fun playTone(toneType: Int, durationMs: Int = 120) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            tg.startTone(toneType, durationMs)
            // ToneGenerator releases automatically after tone finishes
        } catch (_: Throwable) {}
    }

    fun vibrate(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (_: Throwable) {}
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Easter Egg Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThemeEasterEggDialog(
    egg: EasterEggType,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .padding(20.dp)
            ) {
                when (egg) {
                    is EasterEggType.CyberpunkRelic -> CyberpunkEasterEggContent(onDismiss = onDismiss)
                    is EasterEggType.IronManProtocol -> IronManEasterEggContent(onDismiss = onDismiss)
                    is EasterEggType.OnePieceGear5 -> OnePieceEasterEggContent(onDismiss = onDismiss)
                    is EasterEggType.NeoBrutalistOverdrive -> NeoBrutalistEasterEggContent(onDismiss = onDismiss)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. CYBERPUNK 2077 EASTER EGG: RELIC MALFUNCTION & JOHNNY SILVERHAND
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CyberpunkEasterEggContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sandevistanActive by remember { mutableStateOf(false) }
    var relicIntegrity by remember { mutableFloatStateOf(42f) }
    var glitchFlash by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Initial cyberware glitch sound & buzz
        SciFiSoundFx.playTone(ToneGenerator.TONE_CDMA_HIGH_L, 200)
        SciFiSoundFx.vibrate(context, longArrayOf(0, 80, 50, 150))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cp_egg")
    val relicPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (sandevistanActive) 80 else 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "relic_pulse"
    )

    BrutalCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (sandevistanActive) relicPulse else 1f),
        backgroundColor = CyberpunkCard,
        borderColor = if (sandevistanActive) CyberpunkCyan else CyberpunkYellow,
        shadowColor = CyberpunkRed,
        shadowOffset = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CyberpunkScanlines(
                modifier = Modifier.matchParentSize(),
                scanlineColor = CyberpunkCyan.copy(alpha = if (sandevistanActive) 0.18f else 0.08f),
                sweepColor = CyberpunkYellow.copy(alpha = 0.15f)
            )
            CyberCornerTechNotches(modifier = Modifier.matchParentSize(), color = CyberpunkYellow)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrutalBadge(
                        text = if (sandevistanActive) "⚡ SANDEVISTAN OVERCLOCK" else "⚠️ RELIC MALFUNCTION",
                        backgroundColor = if (sandevistanActive) CyberpunkCyan else CyberpunkRed,
                        textColor = Color.Black,
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberpunkCardElevated)
                            .border(1.5.dp, CyberpunkYellow, RoundedCornerShape(6.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CyberpunkYellow, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Biochip icon
                Text(text = "💾", fontSize = 44.sp)

                Spacer(modifier = Modifier.height(10.dp))

                // Iconic Johnny Silverhand Quote
                Text(
                    text = "“WAKE THE F*** UP, SAMURAI.”",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberpunkYellow,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "WE HAVE A CITY TO BURN.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberpunkRed,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Relic Integrity Telemetry Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0A10))
                        .border(1.5.dp, CyberpunkYellow.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "CONSTRUCT: J. SILVERHAND", color = CyberpunkCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${relicIntegrity.toInt()}%", color = if (relicIntegrity < 50f) CyberpunkRed else CyberpunkGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF222230))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(relicIntegrity / 100f)
                                    .fillMaxSize()
                                    .background(if (relicIntegrity < 50f) CyberpunkRed else CyberpunkGreen)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "STATUS: BIOCHIP OVERWRITE IN PROGRESS\nLOCATION: AFTERLIFE // NIGHT CITY",
                            color = Color(0xFFA6ABB9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalButton(
                        onClick = {
                            sandevistanActive = !sandevistanActive
                            SciFiSoundFx.playTone(ToneGenerator.TONE_PROP_PROMPT, 150)
                            SciFiSoundFx.vibrate(context, longArrayOf(0, 40, 40, 80))
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = if (sandevistanActive) CyberpunkCyan else CyberpunkYellow,
                        contentColor = Color.Black
                    ) {
                        Text(
                            text = if (sandevistanActive) "⚡ SANDEVISTAN: ON" else "⚡ SANDEVISTAN",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    BrutalButton(
                        onClick = {
                            relicIntegrity = 100f
                            SciFiSoundFx.playTone(ToneGenerator.TONE_PROP_ACK, 200)
                            SciFiSoundFx.vibrate(context, longArrayOf(0, 100))
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = CyberpunkGreen,
                        contentColor = Color.Black
                    ) {
                        Text(
                            text = "💊 BLOCKERS",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. IRON MAN EASTER EGG: MARK-85 & "I AM IRON MAN"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IronManEasterEggContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var unibeamFired by remember { mutableStateOf(false) }
    var veronicaCalled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SciFiSoundFx.playTone(ToneGenerator.TONE_PROP_BEEP2, 180)
        SciFiSoundFx.vibrate(context, longArrayOf(0, 60, 60, 120))
    }

    BrutalCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = IronManCard,
        borderColor = IronManGold,
        shadowColor = IronManRed,
        shadowOffset = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            JarvisHoloSweep(modifier = Modifier.matchParentSize())
            JarvisCornerTechBrackets(modifier = Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrutalBadge(
                        text = "🦾 MARK-85 PROTOCOL",
                        backgroundColor = IronManGold,
                        textColor = Color.Black,
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(IronManCardElevated)
                            .border(1.5.dp, IronManGold, RoundedCornerShape(6.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = IronManGold, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Arc Reactor Icon
                Text(text = "⚛️", fontSize = 44.sp)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "“I AM IRON MAN.”",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = IronManGoldBright,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "PROOF THAT TONY STARK HAS A HEART ❤️ 3000",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IronManBlueBright,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stark Industries Telemetry
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090A0E))
                        .border(1.5.dp, IronManGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(text = "STARK IND. // JARVIS AI DIAGNOSTIC", color = IronManGold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (veronicaCalled) "🛰️ VERONICA ORBITAL CAGE: DEPLOYED\nHULKBUSTER PROTOCOL: READY"
                                   else "FRIDAY: “House Party Protocol is active, boss. All 42 Mark suits are online.”",
                            color = IronManBlueBright,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        JarvisTelemetryWaveform(modifier = Modifier.height(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalButton(
                        onClick = {
                            unibeamFired = true
                            SciFiSoundFx.playTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 300)
                            SciFiSoundFx.vibrate(context, longArrayOf(0, 150, 50, 200))
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = IronManBlueBright,
                        contentColor = Color.Black
                    ) {
                        Text(
                            text = if (unibeamFired) "💥 UNIBEAM: 100%" else "💥 UNIBEAM",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    BrutalButton(
                        onClick = {
                            veronicaCalled = !veronicaCalled
                            SciFiSoundFx.playTone(ToneGenerator.TONE_PROP_ACK, 200)
                            SciFiSoundFx.vibrate(context, longArrayOf(0, 80))
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = IronManGold,
                        contentColor = Color.Black
                    ) {
                        Text(
                            text = "🛰️ VERONICA",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. ONE PIECE EASTER EGG: GEAR 5 & DRUMS OF LIBERATION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnePieceEasterEggContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var drumsCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        SciFiSoundFx.playTone(ToneGenerator.TONE_PROP_PROMPT, 150)
        SciFiSoundFx.vibrate(context, longArrayOf(0, 100, 50, 100))
    }

    BrutalCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = BrutalCard,
        borderColor = BrutalYellow,
        shadowColor = BrutalOrange,
        shadowOffset = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrutalBadge(
                    text = "🏴‍☠️ WARRIOR OF LIBERATION",
                    backgroundColor = BrutalPurple,
                    textColor = Color.White,
                    fontSize = 11.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrutalCardElevated)
                        .border(1.5.dp, BrutalYellow, RoundedCornerShape(6.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = BrutalYellow, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "👒", fontSize = 44.sp)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "“THE ONE PIECE IS REAL!”",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = BrutalYellow,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "GEAR 5: SUN GOD NIKA // JOYBOY HAS RETURNED!",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrutalPink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bounty Poster Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1C16))
                    .border(2.dp, BrutalYellow, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "DEAD OR ALIVE", fontSize = 12.sp, fontWeight = FontWeight.Black, color = BrutalOrange)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "MONKEY D. LUFFY", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "฿ 3,000,000,000 —", fontSize = 14.sp, fontWeight = FontWeight.Black, color = BrutalYellow)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "DRUMS OF LIBERATION: ${"DUDU-DUN! ".repeat(drumsCount.coerceIn(1, 3))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrutalLime
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            BrutalButton(
                onClick = {
                    drumsCount++
                    SciFiSoundFx.playTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    SciFiSoundFx.vibrate(context, longArrayOf(0, 50, 40, 50, 40, 100))
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalYellow,
                contentColor = Color.Black
            ) {
                Text(
                    text = "🥁 DRUMS OF LIBERATION (DON!)",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. NEO-BRUTALIST EASTER EGG: MAXIMUM OVERDRIVE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NeoBrutalistEasterEggContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SciFiSoundFx.playTone(ToneGenerator.TONE_PROP_PROMPT, 150)
        SciFiSoundFx.vibrate(context, longArrayOf(0, 100, 50, 100))
    }

    BrutalCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = BrutalDarkBg,
        borderColor = BrutalYellow,
        shadowColor = BrutalCyan,
        shadowOffset = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrutalBadge(
                    text = "⚡ MAXIMUM OVERDRIVE",
                    backgroundColor = BrutalYellow,
                    textColor = Color.Black,
                    fontSize = 11.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrutalCardElevated)
                        .border(1.5.dp, BrutalYellow, RoundedCornerShape(6.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = BrutalYellow, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "🔥", fontSize = 44.sp)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "100% UNHINGED NEO-BRUTALISM",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BrutalYellow,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            BrutalHazardTape(
                tapeHeight = 22.dp,
                stripeColor = BrutalPink,
                backgroundColor = BrutalBlack,
                text = "/// MAXIMUM BORDERS /// ZERO BLUR SHADOWS ///"
            )

            Spacer(modifier = Modifier.height(16.dp))

            BrutalButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = BrutalCyan,
                contentColor = Color.Black
            ) {
                Text(
                    text = "AWESOME! KEEP ROCKING 🤘",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}
