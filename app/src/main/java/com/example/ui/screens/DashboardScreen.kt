package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.service.PipraPayService
import com.example.ui.components.PipraPayLogoLockup
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.Amber300
import com.example.ui.theme.BorderZinc700
import com.example.ui.theme.BorderZinc800
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.ContainerDark
import com.example.ui.theme.GhostAmberBg
import com.example.ui.theme.GhostAmberBorder
import com.example.ui.theme.GhostEmeraldBg
import com.example.ui.theme.GhostEmeraldBorder
import com.example.ui.theme.GhostRoseBg
import com.example.ui.theme.GhostRoseBorder
import com.example.ui.theme.PipraTheme
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextZinc300
import com.example.ui.theme.TextZinc400
import com.example.ui.theme.TextZinc500
import com.example.ui.viewmodel.MainViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = PipraTheme.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val providerFilter by viewModel.selectedProviderFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val serverLatency by viewModel.serverLatency.collectAsStateWithLifecycle()
    val serverHealthOk by viewModel.serverHealthOk.collectAsStateWithLifecycle()
    val latestBalances by viewModel.latestBalances.collectAsStateWithLifecycle()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()

    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSmsSimulatorDialog by remember { mutableStateOf(false) }

    // Pulsing animation for subtle live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_and_sync")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val syncRotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotateAngle"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBg)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Unboxed Header: Clean Inline Layout
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PipraPayLogoLockup(
                    iconSize = 34.dp,
                    titleFontSize = 20,
                    subtitle = "Automated MFS Gateway Node",
                    subtitleFontSize = 11
                )

                // Minimal subtle status ghost badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isServiceRunning) GhostEmeraldBg else colors.surfaceCard,
                    border = BorderStroke(1.dp, if (isServiceRunning) GhostEmeraldBorder else colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning) AccentEmerald else colors.textMuted)
                        )
                        Text(
                            text = if (isServiceRunning) "GATEWAY ACTIVE" else "PAUSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = if (isServiceRunning) AccentEmerald else colors.textMuted
                        )
                    }
                }
            }
        }

        // 2. Compact Listener Status Row (Minimal Switch & Glowing Dot)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_status_card"),
                shape = RoundedCornerShape(12.dp),
                color = colors.container,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Subtle glowing green dot with soft pulse ring
                        Box(
                            modifier = Modifier.size(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isServiceRunning) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(AccentEmerald.copy(alpha = pulseAlpha * 0.35f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceRunning) AccentEmerald else colors.textMuted)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = if (isServiceRunning) "Foreground Service Running" else "Foreground Service Paused",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isServiceRunning) colors.textPrimary else colors.textMuted
                            )
                            Text(
                                text = if (isServiceRunning) "Capturing incoming MFS SMS packets" else "SMS background capture inactive",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { viewModel.toggleService(it) },
                        modifier = Modifier.testTag("service_toggle_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentEmerald,
                            checkedBorderColor = AccentEmerald,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.border,
                            uncheckedBorderColor = colors.border
                        )
                    )
                }
            }
        }

        // 3. System Telemetry & Battery Optimization Alert
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Combined System Telemetry Panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("companion_session_card"),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.container,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Device Key Column with click-to-copy
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DEVICE KEY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSubtle,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = settings.deviceKey.ifBlank { "unassigned" },
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 140.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Device Key", settings.deviceKey))
                                            Toast.makeText(context, "Device Key copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy Device Key",
                                            modifier = Modifier.size(12.dp),
                                            tint = colors.textMuted
                                        )
                                    }
                                }
                            }

                            // Target Server Column with inline latency chip
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Text(
                                    text = "TARGET SERVER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSubtle,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = settings.serverBaseUrl
                                            .removePrefix("https://")
                                            .removePrefix("http://")
                                            .trimEnd('/'),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = colors.textPrimary,
                                        modifier = Modifier.widthIn(max = 110.dp)
                                    )

                                    // Inline Latency / Ping chip
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.surfaceCard,
                                        border = BorderStroke(1.dp, colors.border),
                                        modifier = Modifier.clickable {
                                            viewModel.pingServerHealth()
                                            Toast.makeText(context, "Checking latency...", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (serverHealthOk) AccentEmerald else AccentRose)
                                            )
                                            Text(
                                                text = if (serverHealthOk) "${serverLatency ?: 42}ms" else "ERR",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (serverHealthOk) AccentEmerald else AccentRose
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Whitelist Senders / Session info in clean neutral tags
                        val senders = if (settings.whitelistedSenders.isNotEmpty()) settings.whitelistedSenders else listOf("bKash", "NAGAD", "Rocket", "Upay")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FILTER:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSubtle,
                                    letterSpacing = 0.8.sp
                                )
                                senders.forEach { sender ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.surfaceCard,
                                        border = BorderStroke(1.dp, colors.border)
                                    ) {
                                        Text(
                                            text = sender,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.refreshCompanionData()
                                    Toast.makeText(context, "Syncing telemetry...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh Telemetry",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Battery Optimization slim alert ribbon
                if (!isBatteryOptimized) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GhostAmberBg,
                        border = BorderStroke(1.dp, GhostAmberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.BatteryAlert,
                                    contentDescription = "Battery Alert",
                                    tint = Amber300,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Battery Optimization may kill listener",
                                    fontSize = 11.sp,
                                    color = Amber300
                                )
                            }
                            Text(
                                text = "Fix Now",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber300,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            context.startActivity(PipraPayService.getBatteryOptimizationIntent(context))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .testTag("fix_battery_optimization_button")
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Financial & Sync Metrics (Focal Total Inflow & Segmented Counters)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stats_summary_card"),
                shape = RoundedCornerShape(12.dp),
                color = colors.container,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Primary Focal Point: Total Inflow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "TOTAL PROCESSED INFLOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSubtle,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = formatTakaAmount(stats.totalAmount),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-0.8).sp,
                                color = colors.textPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GhostEmeraldBg,
                            border = BorderStroke(1.dp, GhostEmeraldBorder)
                        ) {
                            Text(
                                text = "MFS INFLOW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = AccentEmerald,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    // Sleek Segmented Counter Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Total
                        SegmentedCounterItem(
                            label = "Total",
                            count = stats.totalCount.toString(),
                            countColor = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(colors.border))

                        // Synced
                        SegmentedCounterItem(
                            label = "Synced",
                            count = stats.syncedCount.toString(),
                            countColor = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(colors.border))

                        // Pending (Amber only if > 0)
                        SegmentedCounterItem(
                            label = "Pending",
                            count = stats.pendingCount.toString(),
                            countColor = if (stats.pendingCount > 0) AccentAmber else colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(colors.border))

                        // Failed (Rose only if > 0)
                        SegmentedCounterItem(
                            label = "Failed",
                            count = stats.failedCount.toString(),
                            countColor = if (stats.failedCount > 0) AccentRose else colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. MFS Wallet Grid (Matching Neutral Dark Surfaces)
        item {
            VerifiedBalancesCard(balances = latestBalances)
        }

        // 6. Action Controls: Solid White Primary & Outlined Secondary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action Button
                Button(
                    onClick = { viewModel.triggerManualSync() },
                    enabled = !isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("manual_sync_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isDark) Color.White else Color(0xFF09090B),
                        contentColor = if (colors.isDark) Color.Black else Color.White,
                        disabledContainerColor = colors.surfaceCard,
                        disabledContentColor = colors.textMuted
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = if (!isSyncing) (if (colors.isDark) Color.Black else Color.White) else colors.textMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (isSyncing) syncRotateAngle else 0f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSyncing) "Syncing..." else "Sync Queue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (!isSyncing) (if (colors.isDark) Color.Black else Color.White) else colors.textMuted
                    )
                }

                // Secondary Outlined Ghost Action Button
                OutlinedButton(
                    onClick = { showSmsSimulatorDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("simulate_sms_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.borderInteractive),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = colors.textSecondary
                    )
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = "Simulate SMS",
                        modifier = Modifier.size(16.dp),
                        tint = colors.textMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Simulate SMS",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // 7. Search Toolbar & Minimal Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Modern Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_transactions_input"),
                    placeholder = {
                        Text(
                            text = "Search TrxID, sender, or amount...",
                            fontSize = 13.sp,
                            color = colors.textSubtle
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.textSubtle,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(16.dp),
                                    tint = colors.textMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.borderInteractive,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.container,
                        unfocusedContainerColor = colors.container,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.textPrimary
                    )
                )

                // Single-row horizontal scrolling provider filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val providers = listOf(
                        "ALL" to "All",
                        "BKASH" to "bKash",
                        "NAGAD" to "Nagad",
                        "ROCKET" to "Rocket",
                        "UPAY" to "Upay"
                    )

                    providers.forEach { (key, displayName) ->
                        val isSelected = providerFilter == key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) colors.surfaceCard else colors.container,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) colors.borderInteractive else colors.border
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setProviderFilter(key) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(colors.textPrimary)
                                    )
                                }
                                Text(
                                    text = displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) colors.textPrimary else colors.textSubtle
                                )
                            }
                        }
                    }
                }

                // Compact secondary status filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("ALL", "SYNCED", "PENDING", "FAILED").forEach { status ->
                        val isSelected = statusFilter == status
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) colors.surfaceCard else colors.container,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) colors.borderInteractive else colors.border
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setStatusFilter(status) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (status != "ALL") {
                                    val dotColor = when (status) {
                                        "SYNCED" -> AccentEmerald
                                        "PENDING" -> AccentAmber
                                        "FAILED" -> AccentRose
                                        else -> colors.textSubtle
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                                Text(
                                    text = if (status == "ALL") "All Status" else status,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) colors.textPrimary else colors.textSubtle
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Live Transaction Activity Feed (Feature 3)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("live_activity_feed_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) AccentEmerald else colors.textSubtle)
                    )
                    Text(
                        text = "Live Activity Feed",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.surfaceCard,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = transactions.size.toString(),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (transactions.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAllTransactions() }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AccentRose
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", fontSize = 12.sp, color = AccentRose)
                    }
                }
            }
        }

        // 8. High-Detail Transaction Cards or Empty State
        if (transactions.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("empty_transactions_card"),
                    color = colors.container,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = "Listening",
                                modifier = Modifier.size(22.dp),
                                tint = colors.textMuted
                            )
                        }

                        Text(
                            text = "Listening for incoming transactions...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )

                        Text(
                            text = if (searchQuery.isNotBlank() || providerFilter != "ALL" || statusFilter != "ALL") {
                                "No transactions match the active filters. Try adjusting your search query."
                            } else {
                                "Incoming bKash, Nagad, Rocket, and Upay SMS packets will capture and sync in real-time."
                            },
                            fontSize = 12.sp,
                            color = colors.textSubtle,
                            textAlign = TextAlign.Center
                        )

                        OutlinedButton(
                            onClick = { showSmsSimulatorDialog = true },
                            modifier = Modifier.padding(top = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, colors.borderInteractive),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.surfaceCard,
                                contentColor = colors.textSecondary
                            )
                        ) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = TextZinc400
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test with Sample SMS", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        } else {
            items(transactions, key = { it.trxId }) { trx ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    HighDetailTransactionCard(
                        transaction = trx,
                        onClick = { selectedTransaction = trx },
                        onRetry = { viewModel.resyncTransaction(trx.trxId) }
                    )
                }
            }
        }
    }

    // Detail Bottom Sheet
    selectedTransaction?.let { trx ->
        TransactionDetailSheet(
            transaction = trx,
            onDismiss = { selectedTransaction = null },
            onResync = {
                viewModel.resyncTransaction(trx.trxId)
                selectedTransaction = null
            },
            onDelete = {
                viewModel.deleteTransaction(trx.trxId)
                selectedTransaction = null
            }
        )
    }

    // SMS Simulator Dialog
    if (showSmsSimulatorDialog) {
        SmsSimulatorDialog(
            onDismiss = { showSmsSimulatorDialog = false },
            onInject = { sender, body ->
                val success = viewModel.simulateIncomingSms(sender, body)
                if (success) {
                    Toast.makeText(context, "MFS transaction recognized and saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Rejected: Invalid SMS or OTP alert", Toast.LENGTH_SHORT).show()
                }
                showSmsSimulatorDialog = false
            }
        )
    }
}

/**
 * Sleek segmented counter item with neutral typography
 */
@Composable
private fun SegmentedCounterItem(
    label: String,
    count: String,
    countColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = count,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            color = countColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextZinc500
        )
    }
}

/**
 * High-Detail Transaction Card:
 * - Matching neutral dark surface (#121215 with 1px border #27272a)
 * - Provider badge in neutral surface (#18181b with 1px border #27272a) and bold white text
 * - TrxID: Monospace, 13.sp, Bold, pure white, with copy icon
 * - Sender phone & SIM in muted cool gray
 * - Amount: + ৳ X,XXX.XX in pure white, bold, tight tracking
 * - Ghost status badge (10% tint, small indicator dot)
 */
@Composable
fun HighDetailTransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PipraTheme.colors
    val context = LocalContext.current
    val (statusBg, statusBorder, statusText) = when (transaction.syncStatus) {
        "SYNCED" -> Triple(GhostEmeraldBg, GhostEmeraldBorder, AccentEmerald)
        "PENDING" -> Triple(GhostAmberBg, GhostAmberBorder, AccentAmber)
        "FAILED" -> Triple(GhostRoseBg, GhostRoseBorder, AccentRose)
        else -> Triple(colors.surfaceCard, colors.border, colors.textSubtle)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_card_${transaction.trxId}"),
        shape = RoundedCornerShape(12.dp),
        color = colors.container,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Clean neutral provider badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.provider.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Center: TrxID (bold monospace), sender number, and relative timestamp
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("TrxID", transaction.trxId))
                        Toast.makeText(context, "TrxID copied: ${transaction.trxId}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = transaction.trxId,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy TrxID",
                        modifier = Modifier.size(11.dp),
                        tint = colors.textMuted
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "From: ${transaction.senderNumber}",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colors.surfaceCard,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = "SIM ${transaction.simSlot}",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )
                    }
                }

                Text(
                    text = formatRelativeTime(transaction.timestamp),
                    fontSize = 10.sp,
                    color = colors.textSubtle
                )
            }

            // Right: Amount (+ ৳ X,XXX.XX) and ghost status chip
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatPositiveAmount(transaction.amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.3).sp,
                    color = colors.textPrimary
                )

                if (transaction.balance != null) {
                    Text(
                        text = "Bal: ৳ ${DecimalFormat("#,##0.00").format(transaction.balance)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textSubtle
                    )
                }

                // Ghost status badge (10% tint, 20% border, small dot)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(statusText)
                        )
                        Text(
                            text = transaction.syncStatus,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusText
                        )

                        if (transaction.syncStatus == "FAILED") {
                            Box(
                                modifier = Modifier
                                    .clickable { onRetry() }
                                    .padding(start = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = AccentRose,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats relative timestamp ("Just now", "5m ago", "2h ago", "Yesterday", etc.)
 */
fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 45 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Formats positive inflow amount: "+ ৳ X,XXX.XX"
 */
fun formatPositiveAmount(amount: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "+ ৳ " + formatter.format(amount)
}

/**
 * Formats total amount: "৳ X,XXX.XX"
 */
fun formatTakaAmount(amount: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "৳ " + formatter.format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onResync: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = PipraTheme.colors
    val sheetState = rememberModalBottomSheetState()
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM dd, yyyy • hh:mm:ss a", Locale.getDefault()) }
    val (statusBg, statusBorder, statusText) = when (transaction.syncStatus) {
        "SYNCED" -> Triple(GhostEmeraldBg, GhostEmeraldBorder, AccentEmerald)
        "PENDING" -> Triple(GhostAmberBg, GhostAmberBorder, AccentAmber)
        else -> Triple(GhostRoseBg, GhostRoseBorder, AccentRose)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Details",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusBorder)
                ) {
                    Text(
                        text = transaction.syncStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusText
                    )
                }
            }

            HorizontalDivider(color = colors.border)

            DetailRow("Provider", transaction.provider)
            DetailRow("Sender Key", transaction.senderKey)
            DetailRow("Type", transaction.type)
            DetailRow("Amount", "${transaction.currency} ${DecimalFormat("#,##0.00").format(transaction.amount)}")
            transaction.balance?.let { bal ->
                DetailRow("Post-Trx Balance", "${transaction.currency} ${DecimalFormat("#,##0.00").format(bal)}")
            }
            DetailRow("SIM Slot", "SIM ${transaction.simSlot}")
            DetailRow("Transaction ID", transaction.trxId, isMonospace = true)
            DetailRow("Sender Number", transaction.senderNumber)
            DetailRow("Captured Time", dateFormatter.format(Date(transaction.timestamp)))
            DetailRow("Retry Attempts", "${transaction.retryCount} times")

            transaction.syncErrorMessage?.let { err ->
                DetailRow("Last Error", err, isError = true)
            }

            Text(
                text = "Raw SMS Message",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSubtle,
                letterSpacing = 0.5.sp
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.canvasBg,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = transaction.rawMessage,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.borderInteractive),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }

                Button(
                    onClick = onResync,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isDark) Color.White else Color(0xFF09090B),
                        contentColor = if (colors.isDark) Color.Black else Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (colors.isDark) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isError: Boolean = false,
    isMonospace: Boolean = false
) {
    val colors = PipraTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSubtle,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = if (isError) AccentRose else colors.textPrimary,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun SmsSimulatorDialog(
    onDismiss: () -> Unit,
    onInject: (sender: String, body: String) -> Unit
) {
    val colors = PipraTheme.colors
    val samples = listOf(
        Pair(
            "bKash",
            "You have received Tk 1,500.00 from 01712345678. Ref 01799. Fee Tk 0.00. Balance Tk 6,240.00. TrxID 9ABC123XYZ at 16/09/2026 19:30"
        ),
        Pair(
            "Nagad",
            "Amount: Tk 2,000.00, Sender: 01887654321, TxnID: 7XYZ456, Ref: StorePayment. Balance: Tk 12,300.00"
        ),
        Pair(
            "16216",
            "Cash In Tk 1,200.00 from 01911223344 successful. Fee Tk 0.00. Balance Tk 4,500.00. TxnId: 9876543210. 16/09/2026"
        ),
        Pair(
            "Upay",
            "You have received Tk 1,000.00 from 01600112233. TrxID: UP123456. Fee Tk 0.00. New Balance Tk 3,100.00."
        ),
        Pair(
            "bKash (OTP - Rejected)",
            "Your bKash verification code is 482910. Do not share this OTP with anyone. Ref: Login"
        )
    )

    var customSender by remember { mutableStateOf("bKash") }
    var customBody by remember { mutableStateOf(samples[0].second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.container,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text("Simulate MFS Incoming SMS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pick a sample Bangladeshi MFS SMS or type your own to test parsing, Room storage, and WorkManager sync:",
                    fontSize = 12.sp,
                    color = colors.textMuted
                )

                // Quick sample buttons in neutral dark pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    samples.forEach { (sender, body) ->
                        val isSelected = customBody == body
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) colors.surfaceCard else colors.container,
                            border = BorderStroke(1.dp, if (isSelected) colors.borderInteractive else colors.border),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    customSender = sender
                                    customBody = body
                                }
                        ) {
                            Text(
                                text = sender,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.textPrimary else colors.textMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customSender,
                    onValueChange = { customSender = it },
                    label = { Text("Sender Header", color = colors.textSubtle) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.borderInteractive,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customBody,
                    onValueChange = { customBody = it },
                    label = { Text("SMS Body", color = colors.textSubtle) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.borderInteractive,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onInject(customSender, customBody) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (colors.isDark) Color.White else Color(0xFF09090B),
                    contentColor = if (colors.isDark) Color.Black else Color.White
                )
            ) {
                Text("Process SMS", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMuted)
            }
        }
    )
}

/**
 * Standardized MFS Wallet Grid:
 * - Matching neutral surfaces
 * - Wallet names cleanly in bold text Primary
 * - Status tags ("Waiting for SMS") displayed as neutral pill badges with small muted indicators
 */
@Composable
fun VerifiedBalancesCard(
    balances: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val colors = PipraTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("verified_balances_card"),
        shape = RoundedCornerShape(12.dp),
        color = colors.container,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.textSubtle)
                    )
                    Text(
                        text = "MFS WALLETS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = colors.textSubtle
                    )
                }

                Text(
                    text = "SMS Verified",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSubtle
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val providers = listOf(
                    Pair("bKash", balances["bkash"] ?: balances["BKASH"]),
                    Pair("Nagad", balances["nagad"] ?: balances["NAGAD"]),
                    Pair("Rocket", balances["rocket"] ?: balances["ROCKET"]),
                    Pair("Upay", balances["upay"] ?: balances["UPAY"])
                )

                providers.forEach { (name, balance) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceCard,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.widthIn(min = 108.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            if (balance != null) {
                                Text(
                                    text = "৳ ${DecimalFormat("#,##0.00").format(balance)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textPrimary
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.container,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(AccentEmerald)
                                        )
                                        Text(
                                            text = "Listening",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
