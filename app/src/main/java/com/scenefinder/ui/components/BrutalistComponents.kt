package com.scenefinder.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scenefinder.ui.theme.BrutalBlack
import com.scenefinder.ui.theme.BrutalBorder
import com.scenefinder.ui.theme.BrutalCard
import com.scenefinder.ui.theme.BrutalTextInverse
import com.scenefinder.ui.theme.BrutalYellow

/**
 * Draws a classic Neo-Brutalist hard-edged offset drop shadow (0% blur, 100% opacity).
 */
fun Modifier.brutalShadow(
    offset: Dp = 4.dp,
    color: Color = BrutalBlack,
    cornerRadius: Dp = 10.dp
): Modifier = this.drawBehind {
    val offsetPx = offset.toPx()
    val radiusPx = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(offsetPx, offsetPx),
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(radiusPx, radiusPx)
    )
}

/**
 * Neo-Brutalist styled container with bold border and hard drop shadow.
 */
@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BrutalCard,
    borderColor: Color = BrutalBorder,
    borderWidth: Dp = 2.dp,
    cornerRadius: Dp = 12.dp,
    shadowOffset: Dp = 4.dp,
    shadowColor: Color = BrutalBlack,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .brutalShadow(offset = shadowOffset, color = shadowColor, cornerRadius = cornerRadius)
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .then(clickModifier),
        content = content
    )
}

/**
 * Neo-Brutalist Badge with bold typography, solid high-contrast fill, and crisp border.
 */
@Composable
fun BrutalBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BrutalYellow,
    textColor: Color = BrutalTextInverse,
    borderColor: Color = BrutalBlack,
    borderWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 6.dp,
    fontSize: TextUnit = 10.sp,
    fontWeight: FontWeight = FontWeight.Black
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Tactile Neo-Brutalist Button that physically depresses into its hard shadow when pressed.
 */
@Composable
fun BrutalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BrutalYellow,
    contentColor: Color = BrutalTextInverse,
    borderColor: Color = BrutalBlack,
    borderWidth: Dp = 2.dp,
    cornerRadius: Dp = 10.dp,
    shadowOffset: Dp = 4.dp,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else shadowOffset,
        animationSpec = tween(durationMillis = 60),
        label = "btn_offset"
    )
    val pressTranslation by animateDpAsState(
        targetValue = if (isPressed) shadowOffset else 0.dp,
        animationSpec = tween(durationMillis = 60),
        label = "btn_press"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .offset(x = pressTranslation, y = pressTranslation)
            .brutalShadow(offset = currentOffset, color = BrutalBlack, cornerRadius = cornerRadius)
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
