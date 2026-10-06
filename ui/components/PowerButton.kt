package com.dpibypass.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dpibypass.app.ui.theme.*

@Composable
fun PowerButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "power")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    val activeColor = if (isActive) AccentGreen else YouTubeRed
    val gradient = Brush.radialGradient(
        colors = if (isActive) {
            listOf(AccentGreen, AccentGreenDark)
        } else {
            listOf(YouTubeRed, YouTubeRedDark)
        }
    )

    Box(
        modifier = modifier
            .size(180.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeColor.copy(alpha = glowAlpha * 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Rotating ring
        if (isActive) {
            Canvas(modifier = Modifier.size(190.dp)) {
                drawArc(
                    color = activeColor.copy(alpha = 0.6f),
                    startAngle = ringRotation,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
                drawArc(
                    color = activeColor.copy(alpha = 0.3f),
                    startAngle = ringRotation + 180f,
                    sweepAngle = 60f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Main button
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    ambientColor = activeColor,
                    spotColor = activeColor
                )
                .clip(CircleShape)
                .background(gradient)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Power",
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

private fun Canvas(
    modifier: Modifier = Modifier,
    onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    androidx.compose.foundation.Canvas(modifier = modifier, onDraw = onDraw)
}
