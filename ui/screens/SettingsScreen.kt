package com.dpibypass.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpibypass.app.data.BypassConfig
import com.dpibypass.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: BypassConfig,
    onConfigChange: (BypassConfig) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // DPI Bypass section
            SectionHeader(title = "DPI Bypass", icon = Icons.Default.Shield)

            SettingsSwitch(
                icon = Icons.Default.CallSplit,
                title = "TCP Fragmentation",
                subtitle = "Split TLS ClientHello packets to evade SNI inspection",
                checked = config.tcpFragmentation,
                onCheckedChange = { onConfigChange(config.copy(tcpFragmentation = it)) }
            )

            SettingsSwitch(
                icon = Icons.Default.SwapHoriz,
                title = "Packet Reordering",
                subtitle = "Send fragments out of order to confuse DPI",
                checked = config.packetReordering,
                onCheckedChange = { onConfigChange(config.copy(packetReordering = it)) }
            )

            SettingsSwitch(
                icon = Icons.Default.VisibilityOff,
                title = "SNI Spoofing",
                subtitle = "Inject fake SNI to mislead deep inspection",
                checked = config.sniSpoofing,
                onCheckedChange = { onConfigChange(config.copy(sniSpoofing = it)) }
            )

            SettingsSwitch(
                icon = Icons.Default.Block,
                title = "Block QUIC/HTTP3",
                subtitle = "Force TCP connections (QUIC harder to bypass)",
                checked = config.blockQuic,
                onCheckedChange = { onConfigChange(config.copy(blockQuic = it)) }
            )

            SettingsSwitch(
                icon = Icons.Default.Fingerprint,
                title = "Fake ACK Packets",
                subtitle = "Send dummy TCP acknowledgments",
                checked = config.fakeAck,
                onCheckedChange = { onConfigChange(config.copy(fakeAck = it)) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "Advanced", icon = Icons.Default.Tune)

            SettingsSlider(
                icon = Icons.Default.Straighten,
                title = "Fragment Size",
                subtitle = "Bytes per TCP fragment",
                value = config.fragmentSize.toFloat(),
                valueRange = 2f..64f,
                steps = 30,
                valueLabel = "${config.fragmentSize} B",
                onValueChange = { onConfigChange(config.copy(fragmentSize = it.toInt())) }
            )

            SettingsSlider(
                icon = Icons.Default.Timer,
                title = "Fragment Delay",
                subtitle = "Milliseconds between fragments",
                value = config.fragmentDelay.toFloat(),
                valueRange = 0f..500f,
                steps = 10,
                valueLabel = "${config.fragmentDelay} ms",
                onValueChange = { onConfigChange(config.copy(fragmentDelay = it.toInt())) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "How it works",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "DPI systems inspect SNI in TLS handshakes. By fragmenting packets and reordering them, we prevent the DPI from seeing the complete domain name, allowing YouTube traffic to pass through.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = YouTubeRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = YouTubeRed,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (checked) YouTubeRed.copy(alpha = 0.15f) else DarkElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) YouTubeRed else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = YouTubeRed,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DarkElevated
                )
            )
        }
    }
}

@Composable
private fun SettingsSlider(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(YouTubeRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = YouTubeRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = YouTubeRed,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = YouTubeRed,
                    activeTrackColor = YouTubeRed,
                    inactiveTrackColor = DarkElevated
                )
            )
        }
    }
}
