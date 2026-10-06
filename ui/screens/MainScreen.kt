package com.dpibypass.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpibypass.app.ui.components.*
import com.dpibypass.app.ui.theme.*
import com.dpibypass.app.vpn.VpnState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vpnState: VpnState,
    bytesTransferred: Long,
    packetsProcessed: Long,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val isActive = vpnState == VpnState.CONNECTED
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AnimatedBackground(isActive = isActive)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DPI Bypass",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "YouTube Protection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Power button
            PowerButton(
                isActive = isActive,
                onClick = {
                    if (isActive) onStopVpn() else onStartVpn()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status
            AnimatedContent(
                targetState = isActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith
                            fadeOut(animationSpec = tween(500))
                },
                label = "status"
            ) { active ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (active) StatusActive else StatusInactive,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (active) "Protected" else "Not Protected",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (active) StatusActive else StatusInactive,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (vpnState == VpnState.CONNECTING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    color = YouTubeRed,
                    trackColor = DarkCard
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Stats
            StatsCard(
                icon = Icons.Default.Speed,
                title = "Data Transferred",
                value = formatBytes(bytesTransferred),
                isActive = isActive,
                accentColor = AccentBlue
            )

            Spacer(modifier = Modifier.height(12.dp))

            StatsCard(
                icon = Icons.Default.Shield,
                title = "Packets Processed",
                value = packetsProcessed.toString(),
                isActive = isActive,
                accentColor = AccentGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            StatsCard(
                icon = Icons.Default.Lock,
                title = "Encryption",
                value = if (isActive) "TLS Fragmented" else "Standard",
                isActive = isActive,
                accentColor = YouTubeRed
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Active strategies
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Active Strategies",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        listOf(
                            "TCP Fragmentation" to isActive,
                            "SNI Spoofing" to isActive,
                            "Packet Reorder" to isActive,
                            "QUIC Block" to isActive,
                            "Fake ACK" to isActive
                        )
                    ) { (name, active) ->
                        StrategyChip(
                            text = name,
                            isActive = active
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

private val CircleShape = androidx.compose.foundation.shape.CircleShape
