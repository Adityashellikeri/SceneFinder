package com.scenefinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AgeRatingStyle(
    val color: Color,
    val text: String
)

fun getAgeRatingStyle(rating: String?): AgeRatingStyle {
    val clean = (rating ?: "13+").trim().uppercase()
    val color = when {
        clean.contains("18") || clean.contains("R18") || clean.contains("ADULT") || clean.contains("NC-17") -> Color(0xFFFF3B30) // Red
        clean.contains("16") || clean == "R" || clean.contains("MA") || clean.contains("17") -> Color(0xFFFF9500) // Orange
        clean.contains("13") || clean.contains("PG-13") || clean.contains("14") -> Color(0xFF00B4D8) // Cyan / Blue
        clean.contains("PG") || clean == "G" || clean.contains("ALL") || clean.contains("KIDS") -> Color(0xFF10B981) // Green
        else -> Color(0xFF8E8E93)
    }
    return AgeRatingStyle(color = color, text = clean)
}

@Composable
fun AgeRatingBadge(
    rating: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 8.5.sp
) {
    if (rating.isNullOrBlank()) return
    val style = remember(rating) { getAgeRatingStyle(rating) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(style.color.copy(alpha = 0.22f))
            .border(0.5.dp, style.color.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.5.dp, vertical = 1.5.dp)
    ) {
        Text(
            text = style.text,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = style.color,
            letterSpacing = 0.3.sp
        )
    }
}
