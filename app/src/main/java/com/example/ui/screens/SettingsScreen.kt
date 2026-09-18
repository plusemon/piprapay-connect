package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.components.PipraConfirmationDialog
import com.example.ui.components.PipraPayIcon
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.BorderZinc800
import com.example.ui.theme.BorderZinc700
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.ContainerDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GhostEmeraldBg
import com.example.ui.theme.GhostEmeraldBorder
import com.example.ui.theme.GhostRoseBg
import com.example.ui.theme.GhostRoseBorder
import com.example.ui.theme.PipraTheme
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
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionTestState.collectAsStateWithLifecycle()
    val settingsSaveState by viewModel.settingsSaveState.collectAsStateWithLifecycle()
    val isSaving = settingsSaveState is SettingsSaveState.Loading
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val colors = PipraTheme.colors

    var serverUrl by remember(settings.serverBaseUrl) { mutableStateOf(settings.serverBaseUrl) }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var deviceKey by remember(settings.deviceKey) { mutableStateOf(settings.deviceKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showClearLogsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBg)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Merchant Profile Header Card (Primary Account Identifier Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merchant_profile_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            // Merchant Avatar placeholder with initials or merchant icon
                            val initials = remember(settings.accountName, accountInfo?.fullname) {
                                val name = (accountInfo?.fullname ?: settings.accountName).trim()
                                if (name.isNotBlank()) {
                                    val parts = name.split(" ").filter { it.isNotBlank() }
                                    if (parts.size >= 2) {
                                        "${parts[0].first()}${parts[1].first()}".uppercase()
                                    } else {
                                        name.take(2).uppercase()
                                    }
                                } else "PP"
                            }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (colors.isDark) Color(0xFF27272A) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (colors.isDark) Color.White else Color(0xFF0F172A)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                val displayName = (accountInfo?.fullname ?: settings.accountName).ifBlank { "Merchant Node" }
                                val displayEmailOrHost = (accountInfo?.email ?: settings.accountEmail).ifBlank {
                                    settings.serverBaseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
                                }

                                Text(
                                    text = displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = displayEmailOrHost,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textMuted,
                                    maxLines = 1
                                )
                            }
                        }

                        // Refresh companion account data
                        IconButton(
                            onClick = {
                                viewModel.refreshCompanionData()
                                Toast.makeText(context, "Refreshing merchant profile...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Profile",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    // Badges row: Active Host badge & Stats chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Active Host Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GhostEmeraldBg,
                            border = BorderStroke(1.dp, GhostEmeraldBorder)
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
                                        .background(AccentEmerald)
                                )
                                Text(
                                    text = "Active Host",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp,
                                    color = AccentEmerald
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Quick stats chip (Total Synced)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceCard,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    text = "${stats.syncedCount} Synced",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Device Key Chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceCard,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    text = settings.deviceKey.takeLast(8),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 0. Theme & Appearance Section (Preferences DataStore)
        item {
            SettingsSectionHeader(title = "APPEARANCE & DISPLAY")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("appearance_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDarkMode) Color(0xFF6366F1).copy(alpha = 0.15f)
                                        else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "Theme Icon",
                                    tint = if (isDarkMode) Color(0xFF818CF8) else Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Dark Mode",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = colors.surfaceCard
                                    ) {
                                        Text(
                                            text = if (isDarkMode) "DARK" else "LIGHT",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                                Text(
                                    text = if (isDarkMode)
                                        "High-contrast dark canvas enabled across all screens"
                                    else
                                        "Clean high-readability light canvas active",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentEmerald,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.border
                            ),
                            modifier = Modifier.testTag("dark_mode_toggle")
                        )
                    }
                }
            }
        }

        // 1. PipraPay API & Server Configuration Section
        item {
            SettingsSectionHeader(title = "ENDPOINT CONFIGURATION")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("server_config_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, colors.border),
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
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = colors.textPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceCard,
                            unfocusedContainerColor = colors.surfaceCard,
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedLabelColor = colors.textMuted,
                            unfocusedLabelColor = colors.textMuted,
                            focusedPlaceholderColor = colors.textSubtle,
                            unfocusedPlaceholderColor = colors.textSubtle
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
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = colors.textPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }, enabled = !isSaving) {
                                Icon(
                                    if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key visibility",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceCard,
                            unfocusedContainerColor = colors.surfaceCard,
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedLabelColor = colors.textMuted,
                            unfocusedLabelColor = colors.textMuted,
                            focusedPlaceholderColor = colors.textSubtle,
                            unfocusedPlaceholderColor = colors.textSubtle
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
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = colors.textPrimary),
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
                                        tint = colors.textMuted,
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
                            focusedContainerColor = colors.surfaceCard,
                            unfocusedContainerColor = colors.surfaceCard,
                            focusedBorderColor = AccentEmerald,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedLabelColor = colors.textMuted,
                            unfocusedLabelColor = colors.textMuted,
                            focusedPlaceholderColor = colors.textSubtle,
                            unfocusedPlaceholderColor = colors.textSubtle
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
                                containerColor = if (colors.isDark) Color.White else Color(0xFF09090B),
                                contentColor = if (colors.isDark) Color.Black else Color.White,
                                disabledContainerColor = colors.border,
                                disabledContentColor = colors.textMuted
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
                                    color = colors.textMuted
                                )
                            } else {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (colors.isDark) Color.Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Save",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (colors.isDark) Color.Black else Color.White
                                )
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
                                containerColor = colors.surfaceCard,
                                contentColor = colors.textPrimary
                            ),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            if (connectionState is ConnectionTestState.Testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentEmerald
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp, color = colors.textMuted)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.textPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ping Test", fontSize = 12.sp, color = colors.textPrimary)
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

        // Panel Session Management Card
        item {
            SettingsSectionHeader(title = "PANEL SESSION")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_management_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: Setup Wizard (Re-run onboarding)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup Wizard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Re-open initial pairing and configuration steps",
                                fontSize = 11.sp,
                                color = colors.textMuted
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
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.surfaceCard,
                                contentColor = colors.textPrimary
                            ),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text("Restart", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        }
                    }

                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    // Row 2: Clear Local SMS Logs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clear Local SMS Logs",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Wipe local transaction history, keep panel session",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }

                        OutlinedButton(
                            onClick = { showClearLogsDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("clear_logs_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.surfaceCard,
                                contentColor = AccentRose
                            ),
                            border = BorderStroke(1.dp, AccentRose.copy(alpha = 0.4f))
                        ) {
                            Text("Clear Logs", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AccentRose)
                        }
                    }

                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    // Row 3: Switch Merchant Panel / Disconnect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Switch Merchant Panel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Disconnect node and purge credentials",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }

                        OutlinedButton(
                            onClick = { showDisconnectDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("switch_panel_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.surfaceCard,
                                contentColor = AccentRose
                            ),
                            border = BorderStroke(1.dp, AccentRose.copy(alpha = 0.4f))
                        ) {
                            Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AccentRose)
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
                    containerColor = colors.container
                ),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
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
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Automated MFS Gateway Node",
                                    fontSize = 10.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surfaceCard,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = colors.textMuted
                            )
                        }
                    }

                    HorizontalDivider(
                        color = colors.border,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // GitHub Repo setting (Read-Only)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GitHub Update Repository",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = settings.githubRepo,
                                    fontSize = 13.sp,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surfaceCard,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    text = "Read-Only",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Update State block
                        val updateState by viewModel.updateState.collectAsStateWithLifecycle()

                        when (val state = updateState) {
                            is com.example.util.UpdateState.Idle -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Search GitHub for newer releases",
                                        fontSize = 11.sp,
                                        color = colors.textMuted
                                    )
                                    Button(
                                        onClick = { viewModel.checkForUpdates() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp).testTag("check_update_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (colors.isDark) Color.White else Color(0xFF09090B),
                                            contentColor = if (colors.isDark) Color.Black else Color.White
                                        )
                                    ) {
                                        Text("Check for Updates", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is com.example.util.UpdateState.Checking -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AccentEmerald
                                    )
                                    Text(
                                        text = "Checking GitHub releases...",
                                        fontSize = 12.sp,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            is com.example.util.UpdateState.UpToDate -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StatusSynced,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "App is up to date",
                                            fontSize = 12.sp,
                                            color = StatusSynced,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.checkForUpdates() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = colors.surfaceCard,
                                            contentColor = colors.textPrimary
                                        ),
                                        border = BorderStroke(1.dp, colors.border)
                                    ) {
                                        Text("Check Again", fontSize = 11.sp)
                                    }
                                }
                            }
                            is com.example.util.UpdateState.UpdateAvailable -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AccentEmerald.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.25f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "New Version Available: ${state.versionName}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentEmerald
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = AccentEmerald
                                                ) {
                                                    Text(
                                                        text = "UPDATE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Release Notes:\n${state.releaseNotes}",
                                                fontSize = 11.sp,
                                                color = colors.textPrimary,
                                                maxLines = 4,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.downloadUpdate(state.downloadUrl, state.fileName) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("download_update_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AccentEmerald,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Download & Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.resetUpdateState() },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = colors.surfaceCard,
                                                contentColor = colors.textPrimary
                                            ),
                                            border = BorderStroke(1.dp, colors.border)
                                        ) {
                                            Text("Dismiss", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            is com.example.util.UpdateState.Downloading -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Downloading update...",
                                            fontSize = 12.sp,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${(state.progress * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            color = colors.textMuted,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = AccentEmerald,
                                        trackColor = colors.border
                                    )
                                }
                            }
                            is com.example.util.UpdateState.Downloaded -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StatusSynced.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, StatusSynced.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = StatusSynced,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Download complete! Ready to install.",
                                                fontSize = 12.sp,
                                                color = StatusSynced,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.installUpdate(state.fileUri) },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("install_update_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = StatusSynced,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Install Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.resetUpdateState() },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = colors.surfaceCard,
                                                contentColor = colors.textPrimary
                                            ),
                                            border = BorderStroke(1.dp, colors.border)
                                        ) {
                                            Text("Cancel", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            is com.example.util.UpdateState.Error -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StatusFailed.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, StatusFailed.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = StatusFailed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = state.message,
                                                fontSize = 12.sp,
                                                color = StatusFailed,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.checkForUpdates() },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = StatusFailed,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.resetUpdateState() },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(38.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = colors.surfaceCard,
                                                contentColor = colors.textPrimary
                                            ),
                                            border = BorderStroke(1.dp, colors.border)
                                        ) {
                                            Text("Dismiss", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDisconnectDialog) {
        PipraConfirmationDialog(
            title = "Disconnect Merchant Account?",
            message = "Active telemetry and background SMS capture will be stopped immediately. Stored credentials will be wiped from this device.",
            confirmLabel = "Disconnect",
            cancelLabel = "Keep Connected",
            onConfirm = {
                showDisconnectDialog = false
                viewModel.disconnectMerchantSession {
                    Toast.makeText(context, "Merchant panel disconnected", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showDisconnectDialog = false },
            testTag = "disconnect_merchant_dialog"
        )
    }

    if (showClearLogsDialog) {
        PipraConfirmationDialog(
            title = "Purge Local SMS Cache?",
            message = "Are you sure you want to delete all cached SMS records from local storage? This action cannot be undone.",
            confirmLabel = "Purge Cache",
            cancelLabel = "Cancel",
            onConfirm = {
                showClearLogsDialog = false
                viewModel.clearLocalSmsLogs {
                    Toast.makeText(context, "Local transaction logs wiped", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showClearLogsDialog = false },
            testTag = "clear_logs_dialog"
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = PipraTheme.colors
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = colors.textMuted,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

