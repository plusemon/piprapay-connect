package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.BorderZinc800
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.GhostAmberBg
import com.example.ui.theme.GhostAmberBorder
import com.example.ui.theme.GhostEmeraldBg
import com.example.ui.theme.GhostEmeraldBorder
import com.example.ui.theme.GhostRoseBg
import com.example.ui.theme.GhostRoseBorder
import com.example.ui.theme.PipraTheme
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextZinc400
import com.example.ui.theme.TextZinc500
import com.example.ui.viewmodel.ServerSyncHealth
import com.example.ui.viewmodel.ServerSyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Returns corresponding semantic colors for the health state:
 * - Green (AccentEmerald) for HEALTHY
 * - Amber (AccentAmber) for WARNING (syncing/pending/slow)
 * - Red (AccentRose) for ERROR (offline/unreachable/failed)
 */
fun getHealthAccentColor(status: ServerSyncStatus): Color = when (status) {
    ServerSyncStatus.HEALTHY -> AccentEmerald
    ServerSyncStatus.WARNING -> AccentAmber
    ServerSyncStatus.ERROR -> AccentRose
}

fun getHealthGhostBgColor(status: ServerSyncStatus): Color = when (status) {
    ServerSyncStatus.HEALTHY -> GhostEmeraldBg
    ServerSyncStatus.WARNING -> GhostAmberBg
    ServerSyncStatus.ERROR -> GhostRoseBg
}

fun getHealthGhostBorderColor(status: ServerSyncStatus): Color = when (status) {
    ServerSyncStatus.HEALTHY -> GhostEmeraldBorder
    ServerSyncStatus.WARNING -> GhostAmberBorder
    ServerSyncStatus.ERROR -> GhostRoseBorder
}

/**
 * Small persistent status dot component for the header.
 * Changes color (green/amber/red) based on real-time server connectivity and sync health.
 */
@Composable
fun HeaderStatusDot(
    health: ServerSyncHealth,
    modifier: Modifier = Modifier,
    dotSize: Dp = 7.dp,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val colors = PipraTheme.colors
    val accentColor = getHealthAccentColor(health.status)
    val bgColor = getHealthGhostBgColor(health.status)
    val borderColor = getHealthGhostBorderColor(health.status)

    // Gentle pulse animation for the status dot
    val infiniteTransition = rememberInfiniteTransition(label = "header_dot_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulseScale"
    )

    val labelText = when (health.status) {
        ServerSyncStatus.HEALTHY -> if (health.latencyMs != null) "LIVE • ${health.latencyMs}ms" else "LIVE"
        ServerSyncStatus.WARNING -> {
            if (health.isSyncing) "SYNCING..."
            else if (health.pendingCount > 0) "${health.pendingCount} PENDING"
            else "DEGRADED"
        }
        ServerSyncStatus.ERROR -> {
            if (!health.isOnline) "OFFLINE"
            else if (!health.serverHealthOk) "DISCONNECTED"
            else if (health.failedCount > 0) "${health.failedCount} ERR"
            else "ERROR"
        }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("header_status_dot"),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Pulsing Dot Element
            Box(
                modifier = Modifier
                    .size(dotSize + 8.dp)
                    .testTag("header_status_dot_indicator"),
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse Aura
                Box(
                    modifier = Modifier
                        .size(dotSize + 8.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = pulseAlpha * 0.35f))
                )

                // Core Solid Status Dot
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            if (showLabel) {
                Text(
                    text = labelText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = accentColor,
                    modifier = Modifier.testTag("header_status_dot_label")
                )
            }
        }
    }
}

/**
 * Diagnostic popup modal opened by clicking the header status dot.
 * Displays detailed real-time connectivity and sync health metrics.
 */
@Composable
fun HeaderHealthDetailsDialog(
    health: ServerSyncHealth,
    serverUrl: String,
    onDismiss: () -> Unit,
    onPingServer: () -> Unit,
    onTriggerSync: () -> Unit
) {
    val colors = PipraTheme.colors
    val accentColor = getHealthAccentColor(health.status)
    val statusTitle = when (health.status) {
        ServerSyncStatus.HEALTHY -> "Gateway Healthy"
        ServerSyncStatus.WARNING -> "Sync in Progress / Warning"
        ServerSyncStatus.ERROR -> "Connectivity / Sync Issue"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = "Sync & Server Health",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Summary Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = getHealthGhostBgColor(health.status),
                    border = BorderStroke(1.dp, getHealthGhostBorderColor(health.status)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (health.status) {
                                ServerSyncStatus.HEALTHY -> Icons.Default.Check
                                ServerSyncStatus.WARNING -> Icons.Default.Sync
                                ServerSyncStatus.ERROR -> Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = statusTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = accentColor
                            )
                            Text(
                                text = health.summaryText,
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                // Diagnostics Grid
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.container,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HealthMetricRow(
                            label = "Internet Connectivity",
                            value = if (health.isOnline) "Connected" else "Offline",
                            valueColor = if (health.isOnline) AccentEmerald else AccentRose
                        )
                        HealthMetricRow(
                            label = "Server Reachability",
                            value = if (health.serverHealthOk) "HTTP 200 OK" else "Unreachable",
                            valueColor = if (health.serverHealthOk) AccentEmerald else AccentRose
                        )
                        HealthMetricRow(
                            label = "Roundtrip Latency",
                            value = if (health.latencyMs != null) "${health.latencyMs} ms" else "Unknown",
                            valueColor = if (health.latencyMs != null && health.latencyMs <= 500) colors.textPrimary else AccentAmber
                        )
                        HealthMetricRow(
                            label = "Pending in Queue",
                            value = "${health.pendingCount} packet(s)",
                            valueColor = if (health.pendingCount == 0) AccentEmerald else AccentAmber
                        )
                        HealthMetricRow(
                            label = "Sync Failures",
                            value = "${health.failedCount} error(s)",
                            valueColor = if (health.failedCount == 0) AccentEmerald else AccentRose
                        )
                        HealthMetricRow(
                            label = "Synced Total",
                            value = "${health.syncedCount} packet(s)",
                            valueColor = colors.textPrimary
                        )
                        HealthMetricRow(
                            label = "Server Endpoint",
                            value = serverUrl.ifBlank { "Not Configured" },
                            valueColor = colors.textMuted,
                            isMonospace = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPingServer()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.textPrimary,
                    contentColor = colors.canvasBg
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ping Server", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onTriggerSync()
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sync Now", color = colors.textPrimary, fontSize = 13.sp)
            }
        },
        containerColor = colors.surfaceCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun HealthMetricRow(
    label: String,
    value: String,
    valueColor: Color,
    isMonospace: Boolean = false
) {
    val colors = PipraTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = valueColor
        )
    }
}
