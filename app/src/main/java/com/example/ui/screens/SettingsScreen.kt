package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.service.PipraPayService
import com.example.ui.components.OemOptimizationModal
import com.example.ui.components.PipraPayIcon
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BorderZinc800
import com.example.ui.theme.BorderZinc700
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.ContainerDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GhostRoseBg
import com.example.ui.theme.GhostRoseBorder
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSynced
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextZinc300
import com.example.ui.theme.TextZinc400
import com.example.ui.theme.TextZinc500
import com.example.ui.viewmodel.ConnectionTestState
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SettingsSaveState

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionTestState.collectAsStateWithLifecycle()
    val settingsSaveState by viewModel.settingsSaveState.collectAsStateWithLifecycle()
    val isSaving = settingsSaveState is SettingsSaveState.Loading

    var serverUrl by remember(settings.serverBaseUrl) { mutableStateOf(settings.serverBaseUrl) }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var deviceKey by remember(settings.deviceKey) { mutableStateOf(settings.deviceKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. PipraPay API & Server Configuration Section
        item {
            SettingsSectionHeader(title = "ENDPOINT CONFIGURATION")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("server_config_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ContainerDark),
                border = BorderStroke(1.dp, BorderZinc800),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Server Base URL
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            if (settingsSaveState is SettingsSaveState.Error) viewModel.resetSettingsSaveState()
                        },
                        enabled = !isSaving,
                        label = { Text("Server URL", fontSize = 12.sp) },
                        placeholder = { Text("https://pay.emon.bd/", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = TextZinc400,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = TextWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = BorderZinc800,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = TextZinc400,
                            unfocusedLabelColor = TextZinc400,
                            focusedPlaceholderColor = TextZinc500,
                            unfocusedPlaceholderColor = TextZinc500
                        )
                    )

                    // Merchant API Key
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            if (settingsSaveState is SettingsSaveState.Error) viewModel.resetSettingsSaveState()
                        },
                        enabled = !isSaving,
                        label = { Text("Merchant API Key", fontSize = 12.sp) },
                        placeholder = { Text("pipra_live_...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = TextZinc400,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = TextWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }, enabled = !isSaving) {
                                Icon(
                                    if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key visibility",
                                    tint = TextZinc400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = BorderZinc800,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = TextZinc400,
                            unfocusedLabelColor = TextZinc400,
                            focusedPlaceholderColor = TextZinc500,
                            unfocusedPlaceholderColor = TextZinc500
                        )
                    )

                    // Device Key / POS Identifier
                    OutlinedTextField(
                        value = deviceKey,
                        onValueChange = {
                            deviceKey = it
                            if (settingsSaveState is SettingsSaveState.Error) viewModel.resetSettingsSaveState()
                        },
                        enabled = !isSaving,
                        label = { Text("Device Key (POS ID)", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = TextZinc400,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = TextWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_key_input"),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(deviceKey))
                                        Toast.makeText(context, "Device Key copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Device Key",
                                        tint = TextZinc400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val newKey = viewModel.generateNewDeviceKey()
                                        deviceKey = newKey
                                        Toast.makeText(context, "New key generated: $newKey", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = !isSaving,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Autorenew,
                                        contentDescription = "Generate Key",
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = BorderZinc800,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = TextZinc400,
                            unfocusedLabelColor = TextZinc400,
                            focusedPlaceholderColor = TextZinc500,
                            unfocusedPlaceholderColor = TextZinc500
                        )
                    )

                    // Error banner when verification fails
                    AnimatedVisibility(visible = settingsSaveState is SettingsSaveState.Error) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("settings_error_banner"),
                            shape = RoundedCornerShape(8.dp),
                            color = GhostRoseBg,
                            border = BorderStroke(1.dp, GhostRoseBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = (settingsSaveState as? SettingsSaveState.Error)?.message
                                        ?: "Authentication failed: Invalid Merchant API Key.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFB7185),
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Action Buttons (Save with verification & Ping test)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.verifyAndSaveSettings(
                                    url = serverUrl,
                                    apiKey = apiKey,
                                    deviceKey = deviceKey,
                                    onSuccess = {
                                        Toast.makeText(context, "Handshake verified! Settings saved", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            enabled = !isSaving && connectionState !is ConnectionTestState.Testing,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(42.dp)
                                .testTag("save_settings_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                                disabledContainerColor = BorderZinc800,
                                disabledContentColor = TextZinc400
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentEmerald
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Verifying...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextZinc400
                                )
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.testConnection(serverUrl, apiKey)
                            },
                            enabled = !isSaving && connectionState !is ConnectionTestState.Testing,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("test_connection_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCard,
                                contentColor = TextWhite
                            ),
                            border = BorderStroke(1.dp, BorderZinc800)
                        ) {
                            if (connectionState is ConnectionTestState.Testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentEmerald
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp, color = TextZinc400)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextWhite)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ping Test", fontSize = 12.sp, color = TextWhite)
                            }
                        }
                    }

                    // Connection Test Result Pill
                    if (connectionState is ConnectionTestState.Finished) {
                        val result = (connectionState as ConnectionTestState.Finished).result
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.isSuccess) StatusSynced.copy(alpha = 0.1f) else StatusFailed.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, if (result.isSuccess) StatusSynced.copy(alpha = 0.25f) else StatusFailed.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (result.isSuccess) StatusSynced else StatusFailed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = result.message,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (result.isSuccess) StatusSynced else StatusFailed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Battery Optimization Card
        item {
            SettingsSectionHeader(title = "BACKGROUND OPERATION")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("battery_optimization_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ContainerDark),
                border = BorderStroke(1.dp, BorderZinc800),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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
                                color = TextWhite
                            )
                            Text(
                                text = if (isBatteryOptimized) "Exempted • Continuous sync active" else "Restricted • OEM sleep may kill sync",
                                fontSize = 11.sp,
                                color = TextZinc400
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
                                                context.startActivity(PipraPayService.getBatteryOptimizationIntent(context))
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
                                        context.startActivity(PipraPayService.getBatteryOptimizationIntent(context))
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
                        color = BorderZinc800,
                        modifier = Modifier.padding(vertical = 10.dp)
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
                                color = TextWhite
                            )
                            Text(
                                text = "Xiaomi/MIUI, Samsung, Oppo & Vivo 24/7 background persistence",
                                fontSize = 11.sp,
                                color = TextZinc400
                            )
                        }

                        OutlinedButton(
                            onClick = { showOemModal = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("open_oem_modal_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCard,
                                contentColor = TextWhite
                            ),
                            border = BorderStroke(1.dp, BorderZinc800)
                        ) {
                            Text("Guide", fontSize = 11.sp, color = TextWhite)
                        }
                    }
                }
            }
        }

        // Alerts & Audio Feedback Section
        item {
            SettingsSectionHeader(title = "ALERTS & AUDIO FEEDBACK")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alerts_feedback_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ContainerDark),
                border = BorderStroke(1.dp, BorderZinc800),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Haptic Feedback Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = if (settings.hapticEnabled) AccentEmerald else TextZinc400,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Haptic Pulse",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Sharp tactile vibration on incoming SMS capture",
                                    fontSize = 11.sp,
                                    color = TextZinc400
                                )
                            }
                        }

                        Switch(
                            checked = settings.hapticEnabled,
                            onCheckedChange = { viewModel.setHapticEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentEmerald,
                                uncheckedThumbColor = TextZinc400,
                                uncheckedTrackColor = BorderZinc800
                            ),
                            modifier = Modifier.testTag("haptic_toggle")
                        )
                    }

                    HorizontalDivider(
                        color = BorderZinc800,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Audio Tone Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (settings.audioToneEnabled) AccentEmerald else TextZinc400,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Transaction POS Chime",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Crisp dual-tone sound when payment is parsed",
                                    fontSize = 11.sp,
                                    color = TextZinc400
                                )
                            }
                        }

                        Switch(
                            checked = settings.audioToneEnabled,
                            onCheckedChange = { viewModel.setAudioToneEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentEmerald,
                                uncheckedThumbColor = TextZinc400,
                                uncheckedTrackColor = BorderZinc800
                            ),
                            modifier = Modifier.testTag("audio_tone_toggle")
                        )
                    }

                    HorizontalDivider(
                        color = BorderZinc800,
                        modifier = Modifier.padding(vertical = 10.dp)
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
                            .height(36.dp)
                            .testTag("test_alert_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceCard,
                            contentColor = TextWhite
                        ),
                        border = BorderStroke(1.dp, BorderZinc800)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Chime & Haptic Pulse", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    }
                }
            }
        }

        // 3. System Permissions Checklist Card
        item {
            SettingsSectionHeader(title = "PERMISSIONS")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permissions_checklist_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ContainerDark),
                border = BorderStroke(1.dp, BorderZinc800),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    MinimalPermissionRow(
                        icon = Icons.Default.Sms,
                        title = "Receive SMS",
                        subtitle = "Detect incoming bKash, Nagad & Rocket alerts",
                        isGranted = hasSmsReceive
                    )

                    HorizontalDivider(
                        color = BorderZinc800,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    MinimalPermissionRow(
                        icon = Icons.Default.Security,
                        title = "Read SMS",
                        subtitle = "Extract transaction code and amount",
                        isGranted = hasSmsRead
                    )

                    HorizontalDivider(
                        color = BorderZinc800,
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
                        Spacer(modifier = Modifier.height(12.dp))
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
                            Text("Grant Missing Permissions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CanvasBlack)
                        }
                    }
                }
            }
        }

        // 4. Panel Connection & Reset Card
        item {
            SettingsSectionHeader(title = "PANEL SESSION")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_management_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ContainerDark),
                border = BorderStroke(1.dp, BorderZinc800),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Re-run setup row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup Wizard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextWhite
                            )
                            Text(
                                text = "Re-open initial configuration steps",
                                fontSize = 11.sp,
                                color = TextZinc400
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetOnboarding()
                                Toast.makeText(context, "Restarting setup...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("rerun_onboarding_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCard,
                                contentColor = TextWhite
                            ),
                            border = BorderStroke(1.dp, BorderZinc800)
                        ) {
                            Text("Restart", fontSize = 11.sp, color = TextWhite)
                        }
                    }

                    HorizontalDivider(
                        color = BorderZinc800,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Disconnect / Switch Panel row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Switch Merchant Panel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextWhite
                            )
                            Text(
                                text = "Disconnect and link a new account",
                                fontSize = 11.sp,
                                color = TextZinc400
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.updateSettings("https://api.piprapay.com/", "", viewModel.generateNewDeviceKey())
                                viewModel.resetOnboarding()
                                Toast.makeText(context, "Disconnected panel", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("switch_panel_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceCard,
                                contentColor = StatusFailed
                            ),
                            border = BorderStroke(1.dp, StatusFailed.copy(alpha = 0.4f))
                        ) {
                            Text("Disconnect", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 5. Version & Info Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_version_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ContainerDark
                ),
                border = BorderStroke(1.dp, BorderZinc800),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PipraPayIcon(size = 28.dp)
                        Column {
                            Text(
                                text = "PipraPay Connect",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                            Text(
                                text = "Automated MFS Gateway Node",
                                fontSize = 10.sp,
                                color = TextZinc400
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, BorderZinc800)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = TextZinc400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showOemModal) {
        OemOptimizationModal(onDismiss = { showOemModal = false })
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = TextZinc400,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun MinimalPermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isGranted) AccentEmerald else TextZinc500,
            modifier = Modifier.size(18.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextZinc400
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

