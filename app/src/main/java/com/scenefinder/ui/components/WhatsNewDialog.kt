package com.scenefinder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.scenefinder.ui.theme.LocalAppColors

data class UpdateFeature(
    val badge: String,
    val badgeColor: Color,
    val title: String,
    val description: String,
    val iconEmoji: String
)

@Composable
fun WhatsNewDialog(
    version: String = "2.4.2",
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current

    val headerGradient = Brush.horizontalGradient(
        listOf(Color(0xFF00F2FE), Color(0xFF4FACFE), Color(0xFF0072FF))
    )
    val cardBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        )
    )

    val updateList = listOf(
        UpdateFeature(
            badge = "NEW",
            badgeColor = Color(0xFF10B981),
            title = "Expandable Plot Synopsis",
            description = "Read comprehensive synopses with seamless Read more / Show less toggles across Home, Library, and Overlay cards.",
            iconEmoji = "📖"
        ),
        UpdateFeature(
            badge = "NEW",
            badgeColor = Color(0xFF10B981),
            title = "Live Media Stats & Source Chips",
            description = "Detailed chips display total episodes, chapters, release year, and source recognition service (TMDB / trace.moe / AniList).",
            iconEmoji = "📊"
        ),
        UpdateFeature(
            badge = "NEW",
            badgeColor = Color(0xFF10B981),
            title = "Real-Time Recognition Health",
            description = "Live service connectivity health indicators for trace.moe, TMDB, AniList, and accessibility screen reader.",
            iconEmoji = "⚡"
        ),
        UpdateFeature(
            badge = "IMPROVED",
            badgeColor = Color(0xFF0095F6),
            title = "Setup Readiness Progress Bar",
            description = "Dynamic onboarding readiness tracker showing real-time permission configuration status with animated gradients.",
            iconEmoji = "🎯"
        ),
        UpdateFeature(
            badge = "IMPROVED",
            badgeColor = Color(0xFF0095F6),
            title = "Library & Storage Diagnostics",
            description = "Local SQLite database analytics (Watching, Completed, Planning) with a 1-tap temporary image cache cleaner.",
            iconEmoji = "🧹"
        ),
        UpdateFeature(
            badge = "IMPROVED",
            badgeColor = Color(0xFF0095F6),
            title = "Adaptive 120Hz LTPO Refresh Engine",
            description = "Ultra-smooth high refresh rate during scrolling without draining battery while idle.",
            iconEmoji = "📱"
        ),
        UpdateFeature(
            badge = "FIXED",
            badgeColor = Color(0xFFFF9800),
            title = "Intelligent Library Deduplication",
            description = "Auto-merges duplicate scans favoring enriched metadata, watch progress, and completed statuses.",
            iconEmoji = "🛠️"
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (colors.isDark) Color(0xF4101426) else Color(0xFAFFFFFF),
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with sparkle icon and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00F2FE), Color(0xFF0072FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "What's New",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Ark v$version Release",
                                fontSize = 11.5.sp,
                                color = Color(0xFF00F2FE),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Release notes list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    updateList.forEach { feature ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (colors.isDark) Color(0x2EFFFFFF) else Color(0xFFF3F4F6))
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = feature.iconEmoji,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(feature.badgeColor.copy(alpha = 0.18f))
                                            .border(0.5.dp, feature.badgeColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = feature.badge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = feature.badgeColor
                                        )
                                    }
                                    Text(
                                        text = feature.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = feature.description,
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bottom Action Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(headerGradient),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Got It! Explore Features",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}
