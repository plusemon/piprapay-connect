package com.plusemon.bizlipay.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plusemon.bizlipay.service.BizliPayService
import com.plusemon.bizlipay.ui.components.OemOptimizationModal
import com.plusemon.bizlipay.ui.theme.AccentEmerald
import com.plusemon.bizlipay.ui.theme.CanvasBlack
import com.plusemon.bizlipay.ui.theme.GhostEmeraldBg
import com.plusemon.bizlipay.ui.theme.GhostEmeraldBorder
import com.plusemon.bizlipay.ui.theme.BizliTheme
import com.plusemon.bizlipay.ui.theme.StatusFailed
import com.plusemon.bizlipay.ui.theme.StatusPending
import com.plusemon.bizlipay.ui.theme.StatusSynced
import com.plusemon.bizlipay.ui.viewmodel.MainViewModel

@Composable
fun OperationsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = BizliTheme.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()

    var showOemModal by remember { mutableStateOf(false) }

    // Check system permissions dynamically
    var hasSmsReceive by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasSmsRead by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsReceive = permissions[Manifest.permission.RECEIVE_SMS] ?: hasSmsReceive
        hasSmsRead = permissions[Manifest.permission.READ_SMS] ?: hasSmsRead
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshBatteryOptimizationStatus()
    }

    // Pulse animation for running service
    val infiniteTransition = rememberInfiniteTransition(label = "operations_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBg)
            .testTag("operations_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Master Foreground Service Control Card
        item {
            OperationsSectionHeader(title = "GATEWAY ENGINE")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_control_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, if (isServiceRunning) GhostEmeraldBorder else colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Pulsing dot indicator
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
                                            .background(AccentEmerald.copy(alpha = pulseAlpha * 0.4f))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isServiceRunning) AccentEmerald else colors.textMuted)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Foreground Service",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (isServiceRunning)
                                        "Listening 24/7 for incoming MFS SMS"
                                    else
                                        "Background listener paused",
                                    fontSize = 12.sp,
                                    color = if (isServiceRunning) AccentEmerald else colors.textMuted
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

                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ${if (isServiceRunning) "Running (Persistent)" else "Stopped"}",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = colors.textMuted
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isServiceRunning) GhostEmeraldBg else colors.surfaceCard,
                            border = BorderStroke(1.dp, if (isServiceRunning) GhostEmeraldBorder else colors.border)
                        ) {
                            Text(
                                text = if (isServiceRunning) "ONLINE" else "OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceRunning) AccentEmerald else colors.textMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Battery Optimization Card
        item {
            OperationsSectionHeader(title = "BACKGROUND OPERATION & OEM WORKAROUND")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("battery_optimization_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isBatteryOptimized) StatusSynced.copy(alpha = 0.12f)
                                    else StatusPending.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isBatteryOptimized) Icons.Default.BatteryChargingFull else Icons.Default.Power,
                                contentDescription = null,
                                tint = if (isBatteryOptimized) StatusSynced else StatusPending,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Battery Optimization",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isBatteryOptimized)
                                    "Exempted • Continuous sync active"
                                else
                                    "Restricted • OEM sleep may kill sync",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }

                        if (isBatteryOptimized) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = StatusSynced.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, StatusSynced.copy(alpha = 0.25f)),
                                modifier = Modifier.testTag("open_battery_settings_button")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            try {
                                                context.startActivity(BizliPayService.getBatteryOptimizationIntent(context))
                                            } catch (_: Exception) { }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = StatusSynced,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Exempt",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusSynced
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(BizliPayService.getBatteryOptimizationIntent(context))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Settings error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusPending),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("open_battery_settings_button")
                            ) {
                                Text("Exempt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    HorizontalDivider(
                        color = colors.border,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // OEM Workaround Guide row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OEM Workaround Guide",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Xiaomi/MIUI, Samsung, Oppo & Vivo 24/7 background persistence",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }

                        OutlinedButton(
                            onClick = { showOemModal = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("open_oem_modal_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.surfaceCard,
                                contentColor = colors.textPrimary
                            ),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text("Guide", fontSize = 11.sp, color = colors.textPrimary)
                        }
                    }
                }
            }
        }

        // 3. System Permissions Checklist Card
        item {
            OperationsSectionHeader(title = "TELEPHONY & NOTIFICATION PERMISSIONS")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permissions_checklist_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MinimalPermissionRow(
                        icon = Icons.Default.Sms,
                        title = "Receive SMS",
                        subtitle = "Detect incoming bKash, Nagad & Rocket alerts",
                        isGranted = hasSmsReceive
                    )

                    HorizontalDivider(
                        color = colors.border,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    MinimalPermissionRow(
                        icon = Icons.Default.Security,
                        title = "Read SMS",
                        subtitle = "Extract transaction code and amount",
                        isGranted = hasSmsRead
                    )

                    HorizontalDivider(
                        color = colors.border,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    MinimalPermissionRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Background foreground service status",
                        isGranted = hasNotificationPermission
                    )

                    val allGranted = hasSmsReceive && hasSmsRead && hasNotificationPermission
                    if (!allGranted) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val permissionsToRequest = mutableListOf(
                                    Manifest.permission.RECEIVE_SMS,
                                    Manifest.permission.READ_SMS
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("request_all_permissions_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                        ) {
                            Text(
                                "Grant Missing Permissions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (colors.isDark) CanvasBlack else Color.White
                            )
                        }
                    }
                }
            }
        }

        // 4. Alerts & Audio Feedback Section
        item {
            OperationsSectionHeader(title = "ALERTS & AUDIO FEEDBACK")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alerts_feedback_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Haptic Feedback Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = if (settings.hapticEnabled) AccentEmerald else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Haptic Pulse",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Sharp tactile vibration on incoming SMS capture",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        Switch(
                            checked = settings.hapticEnabled,
                            onCheckedChange = { viewModel.setHapticEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentEmerald,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.border
                            ),
                            modifier = Modifier.testTag("haptic_toggle")
                        )
                    }

                    HorizontalDivider(
                        color = colors.border,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Audio Tone Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (settings.audioToneEnabled) AccentEmerald else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Transaction POS Chime",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Crisp dual-tone sound when payment is parsed",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        Switch(
                            checked = settings.audioToneEnabled,
                            onCheckedChange = { viewModel.setAudioToneEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentEmerald,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.border
                            ),
                            modifier = Modifier.testTag("audio_tone_toggle")
                        )
                    }

                    HorizontalDivider(
                        color = colors.border,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Test Alert Button
                    OutlinedButton(
                        onClick = {
                            viewModel.testAlertFeedback()
                            Toast.makeText(context, "Testing Alert Chime & Haptic...", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("test_alert_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceCard,
                            contentColor = colors.textPrimary
                        ),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.textPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Chime & Haptic Pulse", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showOemModal) {
        OemOptimizationModal(onDismiss = { showOemModal = false })
    }
}

@Composable
private fun OperationsSectionHeader(title: String) {
    val colors = BizliTheme.colors
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = colors.textMuted,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun MinimalPermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    val colors = BizliTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isGranted) AccentEmerald else colors.textSubtle,
            modifier = Modifier.size(18.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) StatusSynced.copy(alpha = 0.12f)
                    else StatusFailed.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = if (isGranted) "Granted" else "Missing",
                tint = if (isGranted) StatusSynced else StatusFailed,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
