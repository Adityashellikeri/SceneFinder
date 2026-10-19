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
import com.scenefinder.ui.theme.BrutalBlack
import com.scenefinder.ui.theme.BrutalCyan
import com.scenefinder.ui.theme.BrutalLime
import com.scenefinder.ui.theme.BrutalOrange
import com.scenefinder.ui.theme.BrutalPink

data class AgeRatingStyle(
    val color: Color,
    val text: String
)

fun getAgeRatingStyle(rating: String?): AgeRatingStyle {
    val clean = (rating ?: "13+").trim().uppercase()
    val color = when {
        clean.contains("18") || clean.contains("R18") || clean.contains("ADULT") || clean.contains("NC-17") -> BrutalPink
        clean.contains("16") || clean == "R" || clean.contains("MA") || clean.contains("17") -> BrutalOrange
        clean.contains("13") || clean.contains("PG-13") || clean.contains("14") -> BrutalCyan
        clean.contains("PG") || clean == "G" || clean.contains("ALL") || clean.contains("KIDS") -> BrutalLime
        else -> Color(0xFFE2E8F0)
    }
    return AgeRatingStyle(color = color, text = clean)
}

/**
 * Neo-Brutalist styled Age Rating Badge: solid punchy background with thick black border and bold black text.
 */
@Composable
fun AgeRatingBadge(
    rating: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 9.sp
) {
    if (rating.isNullOrBlank()) return
    val style = remember(rating) { getAgeRatingStyle(rating) }
    val shape = RoundedCornerShape(5.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(style.color)
            .border(1.2.dp, BrutalBlack, shape)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = style.text,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = BrutalBlack,
            letterSpacing = 0.4.sp
        )
    }
}
