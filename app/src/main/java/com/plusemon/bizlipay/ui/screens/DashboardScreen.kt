package com.plusemon.bizlipay.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plusemon.bizlipay.data.model.TransactionEntity
import com.plusemon.bizlipay.service.BizliPayService
import com.plusemon.bizlipay.ui.components.BizliPayLogoLockup
import com.plusemon.bizlipay.ui.components.HeaderHealthDetailsDialog
import com.plusemon.bizlipay.ui.components.HeaderStatusDot
import com.plusemon.bizlipay.ui.theme.BkashPink
import com.plusemon.bizlipay.ui.theme.Cyan500
import com.plusemon.bizlipay.ui.theme.GhostAmberBg
import com.plusemon.bizlipay.ui.theme.GhostAmberBorder
import com.plusemon.bizlipay.ui.theme.GhostCyanBg
import com.plusemon.bizlipay.ui.theme.GhostCyanBorder
import com.plusemon.bizlipay.ui.theme.GhostEmeraldBg
import com.plusemon.bizlipay.ui.theme.GhostEmeraldBorder
import com.plusemon.bizlipay.ui.theme.GhostIndigoBg
import com.plusemon.bizlipay.ui.theme.GhostIndigoBorder
import com.plusemon.bizlipay.ui.theme.GhostRoseBg
import com.plusemon.bizlipay.ui.theme.GhostRoseBorder
import com.plusemon.bizlipay.ui.theme.Indigo500
import com.plusemon.bizlipay.ui.theme.Indigo600
import com.plusemon.bizlipay.ui.theme.NagadOrange
import com.plusemon.bizlipay.ui.theme.BizliTheme
import com.plusemon.bizlipay.ui.theme.RocketPurple
import com.plusemon.bizlipay.ui.theme.StatusAmber
import com.plusemon.bizlipay.ui.theme.StatusEmerald
import com.plusemon.bizlipay.ui.theme.StatusFailed
import com.plusemon.bizlipay.ui.theme.StatusPending
import com.plusemon.bizlipay.ui.theme.StatusRose
import com.plusemon.bizlipay.ui.theme.StatusSynced
import com.plusemon.bizlipay.ui.theme.UpayYellow
import com.plusemon.bizlipay.ui.viewmodel.MainViewModel
import com.plusemon.bizlipay.ui.viewmodel.ServerSyncStatus
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
    val colors = BizliTheme.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncHealth by viewModel.syncHealth.collectAsStateWithLifecycle()
    val batteryLevel by viewModel.batteryLevel.collectAsStateWithLifecycle()
    val simSlots by viewModel.activeSimSlots.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val providerFilter by viewModel.selectedProviderFilter.collectAsStateWithLifecycle()

    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showHealthDialog by remember { mutableStateOf(false) }

    // Pulsing and spinning animations
    val infiniteTransition = rememberInfiniteTransition(label = "dash_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
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
        label = "syncRotate"
    )

    if (showHealthDialog) {
        HeaderHealthDetailsDialog(
            health = syncHealth,
            serverUrl = settings.serverBaseUrl,
            onDismiss = { showHealthDialog = false },
            onPingServer = { viewModel.pingServerHealth() },
            onTriggerSync = { viewModel.triggerManualSync() }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBg)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header with BizliPay branding and Live Status Pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BizliPayLogoLockup(
                    iconSize = 38.dp,
                    titleFontSize = 22,
                    subtitle = "Device Gateway Node",
                    subtitleFontSize = 11,
                    statusHealth = syncHealth
                )

                // Header quick status dot
                HeaderStatusDot(
                    health = syncHealth,
                    onClick = { showHealthDialog = true }
                )
            }
        }

        // 2. Status Header: Live Status Pill
        item {
            val pillStatusColor = when (syncHealth.status) {
                ServerSyncStatus.HEALTHY -> StatusEmerald
                ServerSyncStatus.WARNING -> StatusAmber
                ServerSyncStatus.ERROR -> StatusRose
            }

            val pillStatusText = when (syncHealth.status) {
                ServerSyncStatus.HEALTHY -> "Connected & Monitoring"
                ServerSyncStatus.WARNING -> "Syncing..."
                ServerSyncStatus.ERROR -> "Disconnected"
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_status_card")
                    .testTag("status_header_pill"),
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, pillStatusColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pulsing status dot
                        Box(
                            modifier = Modifier.size(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (syncHealth.status == ServerSyncStatus.HEALTHY) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(pillStatusColor.copy(alpha = 0.25f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(pillStatusColor)
                            )
                        }

                        Column {
                            Text(
                                text = pillStatusText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.testTag("live_status_text")
                            )
                            Text(
                                text = if (settings.isPaired) "Device UID: ${settings.deviceUid.takeLast(10)}" else "Device Unpaired",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
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
                            checkedTrackColor = Indigo500,
                            checkedBorderColor = Indigo500,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.border,
                            uncheckedBorderColor = colors.border
                        )
                    )
                }
            }
        }

        // 3. Telemetry Cards:
        // A. Total Synced SMS count
        // B. Current Battery % and Last Sync timestamp
        // C. Active SIM slots and carrier names
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Card A: Total Synced SMS Count
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("synced_sms_telemetry_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL SYNCED SMS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = colors.textSubtle
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GhostEmeraldBg,
                                border = BorderStroke(1.dp, GhostEmeraldBorder)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = stats.syncedCount.toString(),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textPrimary,
                                modifier = Modifier.testTag("total_synced_count_text")
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Total Ingested", fontSize = 10.sp, color = colors.textMuted)
                                    Text(text = stats.totalCount.toString(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Pending", fontSize = 10.sp, color = colors.textMuted)
                                    Text(
                                        text = stats.pendingCount.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (stats.pendingCount > 0) StatusAmber else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Row of Card B (Battery & Last Sync) + Card C (Active SIM Slots)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card B: Current Battery % & Last Sync Timestamp
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("battery_telemetry_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = "Battery",
                                    tint = Cyan500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "$batteryLevel%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Cyan500,
                                    modifier = Modifier.testTag("battery_level_text")
                                )
                            }

                            Text(
                                text = "BATTERY & SYNC",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSubtle,
                                letterSpacing = 0.8.sp
                            )

                            val lastSyncStr = remember(settings.lastSyncTimestamp) {
                                if (settings.lastSyncTimestamp > 0) {
                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(settings.lastSyncTimestamp))
                                } else "Never"
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Synced: $lastSyncStr",
                                    fontSize = 11.sp,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("last_sync_timestamp_text")
                                )
                            }
                        }
                    }

                    // Card C: Active SIM slots and carrier names
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sim_telemetry_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = "SIM",
                                    tint = Indigo500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${simSlots.size} Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Indigo500
                                )
                            }

                            Text(
                                text = "ACTIVE SIM SLOTS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSubtle,
                                letterSpacing = 0.8.sp
                            )

                            simSlots.take(2).forEach { sim ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SIM ${sim.slotIndex}:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = sim.carrierName,
                                        fontSize = 11.sp,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Action Buttons:
        // - "Sync Now": Trigger immediate SMS sync worker
        // - "Battery Optimization": Prompt user to whitelist app from battery optimization if needed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "Sync Now" button
                Button(
                    onClick = { viewModel.triggerManualSync() },
                    enabled = !isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("manual_sync_button")
                        .testTag("sync_now_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600,
                        contentColor = Color.White,
                        disabledContainerColor = colors.border,
                        disabledContentColor = colors.textMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (isSyncing) syncRotateAngle else 0f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSyncing) "Syncing..." else "Sync Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                // "Battery Optimization" button
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(BizliPayService.getBatteryOptimizationIntent(context))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open battery settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("battery_optimization_action_button")
                        .testTag("fix_battery_optimization_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (!isBatteryOptimized) StatusAmber else colors.borderInteractive),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!isBatteryOptimized) GhostAmberBg else Color.Transparent,
                        contentColor = if (!isBatteryOptimized) StatusAmber else colors.textPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = "Battery Optimization",
                        tint = if (!isBatteryOptimized) StatusAmber else colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!isBatteryOptimized) "Whitelist App" else "Battery Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (!isBatteryOptimized) StatusAmber else colors.textPrimary
                    )
                }
            }
        }

        // 5. Live Activity Feed Header & Filters
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE ACTIVITY FEED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSubtle,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${transactions.size} Messages",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }
        }

        // Provider Filter Chips (bKash, Nagad, Rocket, Upay)
        item {
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

                providers.forEach { (key, label) ->
                    val isSelected = providerFilter == key
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Indigo600 else colors.surfaceCard,
                        border = BorderStroke(1.dp, if (isSelected) Indigo500 else colors.border),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setProviderFilter(key) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 6. Live Activity Feed: List of recent incoming SMS with colored sender tags
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = colors.textSubtle,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No incoming SMS transactions yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )
                        Text(
                            text = "Incoming payments from bKash, Nagad, Rocket & Upay will appear here live.",
                            fontSize = 11.sp,
                            color = colors.textSubtle,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.simulateIncomingSms(
                                    senderAddress = "bKash",
                                    body = "You have received Tk 500.00 from 01712345678. Ref ORD99. Fee Tk 0.00. Balance Tk 5,500.00. TrxID 9A72BF88 at 25/09/2026 10:15"
                                )
                                Toast.makeText(context, "Simulated bKash payment SMS", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Indigo500),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Indigo500)
                        ) {
                            Text("Simulate Sample Payment", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { trx ->
                SmsActivityFeedItem(
                    transaction = trx,
                    onClick = { selectedTransaction = trx }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Transaction Details Sheet
    if (selectedTransaction != null) {
        val trx = selectedTransaction!!
        ModalBottomSheet(
            onDismissRequest = { selectedTransaction = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.surfaceCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SenderTag(sender = trx.sender)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (trx.is_synced) GhostEmeraldBg else GhostAmberBg,
                        border = BorderStroke(1.dp, if (trx.is_synced) GhostEmeraldBorder else GhostAmberBorder)
                    ) {
                        Text(
                            text = if (trx.is_synced) "SYNCED" else "PENDING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (trx.is_synced) StatusEmerald else StatusAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (trx.amount > 0) {
                    Text(
                        text = "৳ ${String.format(Locale.US, "%.2f", trx.amount)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary
                    )
                }

                if (trx.trxId.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Transaction ID", fontSize = 12.sp, color = colors.textMuted)
                        Text(trx.trxId, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SIM Slot", fontSize = 12.sp, color = colors.textMuted)
                    Text("SIM ${trx.sim_slot}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Timestamp", fontSize = 12.sp, color = colors.textMuted)
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(trx.timestamp))
                    Text(dateFormatted, fontSize = 12.sp, color = colors.textPrimary)
                }

                HorizontalDivider(color = colors.border)

                Text("Raw SMS Content:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSubtle)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.canvasBg,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = trx.message,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Live Activity Feed Item displaying:
 * - Colored sender tag (bKash: Pink, Nagad: Orange, Rocket: Purple, Upay: Yellow)
 * - Message snippet
 * - Sync status badge
 */
@Composable
fun SmsActivityFeedItem(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val colors = BizliTheme.colors
    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("sms_feed_item_${transaction.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SenderTag(sender = transaction.sender)
                    Text(
                        text = "SIM ${transaction.sim_slot}",
                        fontSize = 11.sp,
                        color = colors.textSubtle
                    )
                }

                // Sync status indicator badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (transaction.is_synced) GhostEmeraldBg else GhostAmberBg,
                    border = BorderStroke(1.dp, if (transaction.is_synced) GhostEmeraldBorder else GhostAmberBorder)
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
                                .background(if (transaction.is_synced) StatusEmerald else StatusAmber)
                        )
                        Text(
                            text = if (transaction.is_synced) "Synced" else "Pending",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.is_synced) StatusEmerald else StatusAmber
                        )
                    }
                }
            }

            // Message text snippet
            Text(
                text = transaction.message,
                fontSize = 12.sp,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (transaction.amount > 0) {
                    Text(
                        text = "৳ ${String.format(Locale.US, "%.2f", transaction.amount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Indigo500
                    )
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

/**
 * Colored sender tags according to specification:
 * bKash: Pink
 * Nagad: Orange
 * Rocket: Purple
 * Upay: Yellow
 */
@Composable
fun SenderTag(sender: String) {
    val cleanSender = sender.trim().lowercase()
    val tagColor = when {
        cleanSender.contains("bkash") -> BkashPink
        cleanSender.contains("nagad") -> NagadOrange
        cleanSender.contains("rocket") || cleanSender.contains("16216") -> RocketPurple
        cleanSender.contains("upay") -> UpayYellow
        else -> Cyan500
    }

    val displayName = when {
        cleanSender.contains("bkash") -> "bKash"
        cleanSender.contains("nagad") -> "Nagad"
        cleanSender.contains("rocket") || cleanSender.contains("16216") -> "Rocket"
        cleanSender.contains("upay") -> "Upay"
        else -> sender.ifBlank { "SMS" }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tagColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, tagColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tagColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
