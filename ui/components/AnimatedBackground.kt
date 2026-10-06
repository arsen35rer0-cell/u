package com.dpibypass.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.dpibypass.app.ui.theme.YouTubeRed
import com.dpibypass.app.ui.theme.YouTubeRedDark

@Composable
fun AnimatedBackground(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = if (isActive) 0.4f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension * 0.6f * scale

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    YouTubeRed.copy(alpha = pulseAlpha),
                    YouTubeRedDark.copy(alpha = pulseAlpha * 0.5f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = radius
            ),
            center = Offset(centerX, centerY),
            radius = radius
        )

        // Rotating accent circles
        if (isActive) {
            val orbitRadius = radius * 0.7f
            for (i in 0..5) {
                val angle = Math.toRadians((rotation + i * 60).toDouble())
                val x = centerX + (orbitRadius * Math.cos(angle)).toFloat()
                val y = centerY + (orbitRadius * Math.sin(angle)).toFloat()
                drawCircle(
                    color = YouTubeRed.copy(alpha = 0.3f),
                    radius = 8f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
