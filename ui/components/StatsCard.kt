package com.dpibypass.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.dpibypass.app.ui.theme.*

@Composable
fun StatsCard(
    icon: ImageVector,
    title: String,
    value: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = YouTubeRed
) {
    val animatedValue by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.6f,
        animationSpec = tween(500),
        label = "stats"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        DarkCard,
                        DarkCard.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary.copy(alpha = animatedValue)
            )
        }
    }
}

@Composable
fun StrategyChip(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) YouTubeRed.copy(alpha = 0.2f) else DarkCard,
        animationSpec = tween(300),
        label = "chip"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) YouTubeRed else TextTertiary,
        animationSpec = tween(300),
        label = "border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .then(
                Modifier.background(
                    Brush.horizontalGradient(
                        colors = if (isActive) listOf(
                            YouTubeRed.copy(alpha = 0.1f),
                            YouTubeRedDark.copy(alpha = 0.05f)
                        ) else listOf(Color.Transparent, Color.Transparent)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isActive) YouTubeRed else TextSecondary
        )
    }
}
