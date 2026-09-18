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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.service.PipraPayService
import com.example.ui.theme.BkashPink
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.RocketPurple
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateStroke
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSynced
import com.example.ui.theme.UpayNavy
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
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val providerFilter by viewModel.selectedProviderFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val serverLatency by viewModel.serverLatency.collectAsStateWithLifecycle()
    val serverHealthOk by viewModel.serverHealthOk.collectAsStateWithLifecycle()
    val latestBalances by viewModel.latestBalances.collectAsStateWithLifecycle()

    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSmsSimulatorDialog by remember { mutableStateOf(false) }

    // Pulsing animation for foreground service indicator
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
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Unified Hero Header & Service Switch
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .testTag("live_status_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SlateDark
                ),
                border = BorderStroke(1.dp, SlateStroke),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top row: Brand mark & Enterprise subline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(EmeraldLight, EmeraldPrimary)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "P",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "PipraPay",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = EmeraldLight
                                    )
                                    Text(
                                        text = "Companion",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "ENTERPRISE MFS GATEWAY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextMuted,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Live Gateway Status Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "LIVE GATEWAY",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Middle row: Animated Pulsing Service Switch & Status
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SlateCard,
                        border = BorderStroke(1.dp, SlateStroke.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Animated pulsing green indicator (active) or red/grey (inactive)
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isServiceRunning) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .scale(pulseScale)
                                                .clip(CircleShape)
                                                .background(StatusSynced.copy(alpha = pulseAlpha * 0.4f))
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isServiceRunning) StatusSynced else StatusFailed
                                            )
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (isServiceRunning) "LISTENER ACTIVE" else "LISTENER PAUSED",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (isServiceRunning) StatusSynced else StatusFailed,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = if (isServiceRunning) "Foreground SMS capture running" else "SMS background capture stopped",
                                        fontSize = 11.sp,
                                        color = SlateTextMuted
                                    )
                                }
                            }

                            Switch(
                                checked = isServiceRunning,
                                onCheckedChange = { viewModel.toggleService(it) },
                                modifier = Modifier.testTag("service_toggle_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = Color(0xFFCBD5E1),
                                    uncheckedTrackColor = Color(0xFF334155)
                                )
                            )
                        }
                    }

                    // Bottom Connectivity Row below the switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Device Key with subtle copy icon
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DEVICE KEY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = settings.deviceKey.ifBlank { "unassigned" },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White,
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
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Device Key",
                                        modifier = Modifier.size(13.dp),
                                        tint = EmeraldLight
                                    )
                                }
                            }
                        }

                        // Truncated Server URL + inline Health/Latency Chip
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = "TARGET SERVER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = settings.serverBaseUrl
                                        .removePrefix("https://")
                                        .removePrefix("http://")
                                        .trimEnd('/'),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White,
                                    modifier = Modifier.widthIn(max = 110.dp)
                                )

                                // Inline latency / health chip
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (serverHealthOk) StatusSynced.copy(alpha = 0.16f) else StatusFailed.copy(alpha = 0.16f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (serverHealthOk) StatusSynced.copy(alpha = 0.35f) else StatusFailed.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.clickable {
                                        viewModel.pingServerHealth()
                                        Toast.makeText(context, "Checking server ping...", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (serverHealthOk) StatusSynced else StatusFailed)
                                        )
                                        Text(
                                            text = if (serverHealthOk) "${serverLatency ?: 42}ms" else "ERR",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (serverHealthOk) StatusSynced else StatusFailed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Battery optimization warning if not excluded
                    if (!isBatteryOptimized) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusPending.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, StatusPending.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
                                        tint = StatusPending,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Battery Optimization may kill listener",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            context.startActivity(PipraPayService.getBatteryOptimizationIntent(context))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Could not open battery settings", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("fix_battery_optimization_button")
                                ) {
                                    Text("Fix Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusPending)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Upgraded Metrics & Balance Card (Slate #1E293B, 1px stroke #334155, rounded 20.dp)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stats_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SlateCard
                ),
                border = BorderStroke(1.dp, SlateStroke),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header: TOTAL PROCESSED label & prominent sum with Bengali Taka sign in bold 28.sp typography
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL PROCESSED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextMuted,
                                letterSpacing = 1.2.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "MFS INFLOW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = formatTakaAmount(stats.totalAmount),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    HorizontalDivider(
                        color = SlateStroke.copy(alpha = 0.7f),
                        thickness = 1.dp
                    )

                    // 4-Column row of counters with dedicated status dot accents
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetricsCounterColumn(
                            label = "Total",
                            count = stats.totalCount.toString(),
                            dotColor = Color.White,
                            countColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        MetricsCounterColumn(
                            label = "Synced",
                            count = stats.syncedCount.toString(),
                            dotColor = StatusSynced,
                            countColor = StatusSynced,
                            modifier = Modifier.weight(1f)
                        )
                        MetricsCounterColumn(
                            label = "Pending",
                            count = stats.pendingCount.toString(),
                            dotColor = StatusPending,
                            countColor = StatusPending,
                            modifier = Modifier.weight(1f)
                        )
                        MetricsCounterColumn(
                            label = "Failed",
                            count = stats.failedCount.toString(),
                            dotColor = StatusFailed,
                            countColor = StatusFailed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2.5 Verified Wallet Balances
        item {
            VerifiedBalancesCard(balances = latestBalances)
        }

        // 3. Action Buttons: Sync Queue (Primary Solid) & Simulate SMS (Tonal / Outlined)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary solid action with animated rotating sync icon
                Button(
                    onClick = { viewModel.triggerManualSync() },
                    enabled = !isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("manual_sync_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isSyncing) syncRotateAngle else 0f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSyncing) "Syncing..." else "Sync Queue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Sleek tonal / outlined style with message bubble icon
                OutlinedButton(
                    onClick = { showSmsSimulatorDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("simulate_sms_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateStroke),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SlateCard.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = "Simulate SMS",
                        modifier = Modifier.size(18.dp),
                        tint = EmeraldLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simulate SMS",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 3b. Search Toolbar & Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Rounded-full search bar (30.dp) with clear button and placeholder
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
                            color = SlateTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SlateTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp),
                                    tint = SlateTextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SlateStroke,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Single-row horizontal scrolling chips for filtering (All, bKash, Nagad, Rocket, Upay) with active background tinting
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
                        val brandColor = when (key) {
                            "BKASH" -> BkashPink
                            "NAGAD" -> NagadOrange
                            "ROCKET" -> RocketPurple
                            "UPAY" -> UpayNavy
                            else -> EmeraldPrimary
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) brandColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) brandColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { viewModel.setProviderFilter(key) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (key != "ALL") {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(brandColor)
                                    )
                                }
                                Text(
                                    text = displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurface
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
                        val statusColor = when (status) {
                            "SYNCED" -> StatusSynced
                            "PENDING" -> StatusPending
                            "FAILED" -> StatusFailed
                            else -> MaterialTheme.colorScheme.primary
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setStatusFilter(status) },
                            label = {
                                Text(
                                    text = if (status == "ALL") "All Status" else status,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = statusColor.copy(alpha = 0.16f),
                                selectedLabelColor = statusColor
                            )
                        )
                    }
                }
            }
        }

        // Section Title: Recent Transactions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Transactions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = transactions.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                if (transactions.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAllTransactions() }) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = StatusFailed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", fontSize = 12.sp, color = StatusFailed)
                    }
                }
            }
        }

        // 4. High-Detail Transaction Cards or Empty State
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .testTag("empty_transactions_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = SlateCard.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SlateStroke.copy(alpha = 0.5f))
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
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = "Listening",
                                modifier = Modifier.size(28.dp),
                                tint = EmeraldLight
                            )
                        }

                        Text(
                            text = "Listening for incoming transactions...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (searchQuery.isNotBlank() || providerFilter != "ALL" || statusFilter != "ALL") {
                                "No transactions match the active filters. Try adjusting your search query."
                            } else {
                                "Incoming bKash, Nagad, Rocket, and Upay SMS notifications will appear here and sync in real-time."
                            },
                            fontSize = 12.sp,
                            color = SlateTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Button(
                            onClick = { showSmsSimulatorDialog = true },
                            modifier = Modifier.padding(top = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test with Sample SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
 * 4-column row metrics item with dedicated status dot accent
 */
@Composable
private fun MetricsCounterColumn(
    label: String,
    count: String,
    dotColor: Color,
    countColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = countColor
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = SlateTextMuted
        )
    }
}

/**
 * High-Detail Transaction Card:
 * - Left: Provider badge with brand colors (bKash #E2136E, Nagad #F7941D, Rocket #8C3494, Upay #005696)
 * - Center: TrxID (bold monospace with one-tap copy toast), sender phone (From: 017...), relative timestamp
 * - Right: Amount (+ ৳ X,XXX.XX), status chip (SYNCED in soft green pill, PENDING in glowing yellow, FAILED in red with retry icon)
 */
@Composable
fun HighDetailTransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val providerColor = when (transaction.provider.uppercase().trim()) {
        "BKASH" -> BkashPink
        "NAGAD" -> NagadOrange
        "ROCKET", "16216" -> RocketPurple
        "UPAY" -> UpayNavy
        else -> EmeraldPrimary
    }

    val statusColor = when (transaction.syncStatus) {
        "SYNCED" -> StatusSynced
        "PENDING" -> StatusPending
        "FAILED" -> StatusFailed
        else -> Color.Gray
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_card_${transaction.trxId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Provider badge with brand colors
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = providerColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, providerColor.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.provider.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = providerColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Center: TrxID (bold monospace), sender number, and relative timestamp
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Monospace TrxID with one-tap copy toast
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy TrxID",
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "From: ${transaction.senderNumber}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "SIM ${transaction.simSlot}",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = formatRelativeTime(transaction.timestamp),
                    fontSize = 10.sp,
                    color = SlateTextMuted
                )
            }

            // Right: Amount (+ ৳ X,XXX.XX) and status chip
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatPositiveAmount(transaction.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )

                if (transaction.balance != null) {
                    Text(
                        text = "Bal: ৳ ${DecimalFormat("#,##0.00").format(transaction.balance)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status chip (SYNCED in soft green pill, PENDING in glowing yellow, FAILED with retry)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = transaction.syncStatus,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
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
                                    tint = StatusFailed,
                                    modifier = Modifier.size(12.dp)
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
    val sheetState = rememberModalBottomSheetState()
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM dd, yyyy • hh:mm:ss a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (transaction.syncStatus) {
                        "SYNCED" -> StatusSynced.copy(alpha = 0.2f)
                        "PENDING" -> StatusPending.copy(alpha = 0.2f)
                        else -> StatusFailed.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = transaction.syncStatus,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (transaction.syncStatus) {
                            "SYNCED" -> StatusSynced
                            "PENDING" -> StatusPending
                            else -> StatusFailed
                        }
                    )
                }
            }

            HorizontalDivider()

            DetailRow("Provider", transaction.provider)
            DetailRow("Sender Key", transaction.senderKey)
            DetailRow("Type", transaction.type)
            DetailRow("Amount", "${transaction.currency} ${DecimalFormat("#,##0.00").format(transaction.amount)}")
            transaction.balance?.let { bal ->
                DetailRow("Post-Trx Balance", "${transaction.currency} ${DecimalFormat("#,##0.00").format(bal)}")
            }
            DetailRow("SIM Slot", "SIM ${transaction.simSlot}")
            DetailRow("Transaction ID", transaction.trxId)
            DetailRow("Sender Number", transaction.senderNumber)
            DetailRow("Captured Time", dateFormatter.format(Date(transaction.timestamp)))
            DetailRow("Retry Attempts", "${transaction.retryCount} times")

            transaction.syncErrorMessage?.let { err ->
                DetailRow("Last Error", err, isError = true)
            }

            Text(
                text = "Raw SMS Message",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = transaction.rawMessage,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusFailed)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }

                Button(
                    onClick = onResync,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Now")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) StatusFailed else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.5f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun SmsSimulatorDialog(
    onDismiss: () -> Unit,
    onInject: (sender: String, body: String) -> Unit
) {
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
        title = {
            Text("Simulate MFS Incoming SMS", fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick sample buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    samples.forEach { (sender, body) ->
                        FilterChip(
                            selected = customBody == body,
                            onClick = {
                                customSender = sender
                                customBody = body
                            },
                            label = { Text(sender, fontSize = 11.sp) }
                        )
                    }
                }

                androidx.compose.material3.OutlinedTextField(
                    value = customSender,
                    onValueChange = { customSender = it },
                    label = { Text("Sender Header") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.material3.OutlinedTextField(
                    value = customBody,
                    onValueChange = { customBody = it },
                    label = { Text("SMS Body") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onInject(customSender, customBody) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Process SMS")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun VerifiedBalancesCard(
    balances: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("verified_balances_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                    )
                    Text(
                        text = "LIVE WALLET BALANCES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "SMS Verified",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val providers = listOf(
                    Triple("bKash", BkashPink, balances["bkash"] ?: balances["BKASH"]),
                    Triple("Nagad", NagadOrange, balances["nagad"] ?: balances["NAGAD"]),
                    Triple("Rocket", RocketPurple, balances["rocket"] ?: balances["ROCKET"]),
                    Triple("Upay", UpayNavy, balances["upay"] ?: balances["UPAY"])
                )

                providers.forEach { (name, color, balance) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = color.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
                        modifier = Modifier.widthIn(min = 120.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                            Text(
                                text = if (balance != null) "৳ ${DecimalFormat("#,##0.00").format(balance)}" else "Awaiting SMS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (balance != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
