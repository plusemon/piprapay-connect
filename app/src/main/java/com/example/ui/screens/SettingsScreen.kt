package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import com.example.BuildConfig
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.PipraPayService
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSynced
import com.example.ui.viewmodel.ConnectionTestState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionTestState.collectAsStateWithLifecycle()

    var serverUrl by remember(settings.serverBaseUrl) { mutableStateOf(settings.serverBaseUrl) }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var deviceKey by remember(settings.deviceKey) { mutableStateOf(settings.deviceKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

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
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. PipraPay API & Server Configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("server_config_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "PipraPay Backend Connection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Configure your merchant credentials. The companion app forwards detected SMS transactions to this endpoint.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Environment Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = serverUrl == "https://api.piprapay.com/",
                            onClick = { serverUrl = "https://api.piprapay.com/" },
                            label = { Text("Production", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = serverUrl == "https://staging-api.piprapay.com/",
                            onClick = { serverUrl = "https://staging-api.piprapay.com/" },
                            label = { Text("Staging / Sandbox", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = serverUrl == "http://10.0.2.2:8080/",
                            onClick = { serverUrl = "http://10.0.2.2:8080/" },
                            label = { Text("Localhost", fontSize = 11.sp) }
                        )
                    }

                    // Server Base URL input
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("PipraPay Server Base URL") },
                        placeholder = { Text("https://api.piprapay.com/") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Merchant API Key
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Merchant API Key (Bearer Token)") },
                        placeholder = { Text("pipra_live_...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key visibility"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Device Key / ID
                    OutlinedTextField(
                        value = deviceKey,
                        onValueChange = { deviceKey = it },
                        label = { Text("Device Key / POS Identifier") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_key_input"),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val newKey = viewModel.generateNewDeviceKey()
                                    deviceKey = newKey
                                    Toast.makeText(context, "New Device Key generated: $newKey", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Autorenew,
                                    contentDescription = "Generate Key",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Save & Test Connection Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateSettings(serverUrl, apiKey, deviceKey)
                                Toast.makeText(context, "Settings saved securely!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_settings_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Settings")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.testConnection(serverUrl, apiKey)
                            },
                            enabled = connectionState !is ConnectionTestState.Testing,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("test_connection_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (connectionState is ConnectionTestState.Testing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...")
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Ping")
                            }
                        }
                    }

                    // Connection Test Result Banner
                    if (connectionState is ConnectionTestState.Finished) {
                        val result = (connectionState as ConnectionTestState.Finished).result
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (result.isSuccess) StatusSynced.copy(alpha = 0.12f) else StatusFailed.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (result.isSuccess) StatusSynced else StatusFailed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (result.isSuccess) "Connection Succeeded" else "Connection Failed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (result.isSuccess) StatusSynced else StatusFailed
                                    )
                                    Text(
                                        text = result.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Android Battery Optimization Exemption Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("battery_optimization_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(
                                Icons.Default.Power,
                                contentDescription = null,
                                tint = if (isBatteryOptimized) StatusSynced else StatusPending,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Battery Optimization",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isBatteryOptimized) StatusSynced.copy(alpha = 0.15f) else StatusPending.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isBatteryOptimized) "EXEMPTED" else "OPTIMIZED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBatteryOptimized) StatusSynced else StatusPending
                            )
                        }
                    }

                    Text(
                        text = "Aggressive OEM battery managers (Xiaomi, Samsung, Oppo, Vivo) may kill background SMS listeners when the screen turns off. Exclude PipraPay Companion to ensure 24/7 continuous syncing.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            try {
                                context.startActivity(PipraPayService.getBatteryOptimizationIntent(context))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot launch battery settings directly: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("open_battery_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBatteryOptimized) MaterialTheme.colorScheme.surfaceVariant else StatusPending
                        )
                    ) {
                        Text(
                            text = if (isBatteryOptimized) "Manage Battery Settings" else "Request Battery Exemption",
                            color = if (isBatteryOptimized) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Android System Permissions Checklist
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permissions_checklist_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "System Permissions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    PermissionStatusRow(
                        title = "Receive SMS (RECEIVE_SMS)",
                        description = "Intercepts incoming bKash, Nagad, Rocket, and Upay alerts",
                        isGranted = hasSmsReceive
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    PermissionStatusRow(
                        title = "Read SMS (READ_SMS)",
                        description = "Required on some Android versions to parse message body",
                        isGranted = hasSmsRead
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    PermissionStatusRow(
                        title = "Notifications (POST_NOTIFICATIONS)",
                        description = "Displays ongoing service status and incoming payment popups",
                        isGranted = hasNotificationPermission
                    )

                    val allGranted = hasSmsReceive && hasSmsRead && hasNotificationPermission
                    if (!allGranted) {
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
                                .height(44.dp)
                                .testTag("request_all_permissions_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Grant Missing Permissions", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Onboarding & Panel Reconnection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_management_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Onboarding & Panel Connection",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Need to re-verify permissions, review setup readiness, or connect to a different merchant payment panel? You can re-open the initial setup flow anytime.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.resetOnboarding()
                                Toast.makeText(context, "Restarting onboarding setup...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("rerun_onboarding_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Restart Setup", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.updateSettings("https://api.piprapay.com/", "", viewModel.generateNewDeviceKey())
                                viewModel.resetOnboarding()
                                Toast.makeText(context, "Disconnected panel. Ready to connect a new panel.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("switch_panel_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Switch Panel", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // 5. Version & GitHub Release Information Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_version_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Release & Version",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    Text(
                        text = "PipraPay Companion • Build ${BuildConfig.VERSION_CODE}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Automated CI/CD releases are published through GitHub Actions (.github/workflows/release.yml) upon new version tags or dispatch trigger.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isGranted) StatusSynced.copy(alpha = 0.15f) else StatusFailed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) StatusSynced else StatusFailed,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
