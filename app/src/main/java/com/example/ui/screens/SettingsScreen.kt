package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.PipraPayService
import com.example.ui.components.BizliPayIcon
import com.example.ui.theme.Cyan500
import com.example.ui.theme.GhostEmeraldBg
import com.example.ui.theme.GhostEmeraldBorder
import com.example.ui.theme.GhostRoseBg
import com.example.ui.theme.GhostRoseBorder
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.PipraTheme
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val colors = PipraTheme.colors

    var showUnpairDialog by remember { mutableStateOf(false) }

    val pairedDateStr = remember(settings.pairedTimestamp) {
        if (settings.pairedTimestamp > 0) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(settings.pairedTimestamp))
        } else {
            "Active session"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBg)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Device & Pairing Information
        item {
            SettingsSectionHeader(title = "DEVICE & PAIRING INFORMATION")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("device_pairing_info_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Server URL row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = Cyan500, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Server Base URL", fontSize = 11.sp, color = colors.textSubtle, fontWeight = FontWeight.Bold)
                                Text(
                                    text = settings.serverBaseUrl,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.testTag("settings_server_url_text")
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(settings.serverBaseUrl))
                                Toast.makeText(context, "Server URL copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.textMuted, modifier = Modifier.size(14.dp))
                        }
                    }

                    HorizontalDivider(color = colors.border)

                    // Device UID row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = Indigo500, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Device UID", fontSize = 11.sp, color = colors.textSubtle, fontWeight = FontWeight.Bold)
                                Text(
                                    text = settings.deviceUid.ifBlank { settings.deviceKey },
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.testTag("settings_device_uid_text")
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(settings.deviceUid))
                                Toast.makeText(context, "Device UID copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.textMuted, modifier = Modifier.size(14.dp))
                        }
                    }

                    HorizontalDivider(color = colors.border)

                    // Pairing Timestamp row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Pairing Timestamp", fontSize = 11.sp, color = colors.textSubtle, fontWeight = FontWeight.Bold)
                                Text(
                                    text = pairedDateStr,
                                    fontSize = 12.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.testTag("settings_pairing_timestamp_text")
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GhostEmeraldBg,
                            border = BorderStroke(1.dp, GhostEmeraldBorder)
                        ) {
                            Text(
                                text = "PAIRED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section: Background Sync Service Toggle
        item {
            SettingsSectionHeader(title = "BACKGROUND SYNCHRONIZATION")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_service_toggle_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning) GhostEmeraldBg else colors.canvasBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (isServiceRunning) StatusEmerald else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Background Sync Service",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isServiceRunning) "Running persistent listener" else "Sync worker paused",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { viewModel.toggleService(it) },
                        modifier = Modifier.testTag("background_sync_service_toggle"),
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

        // Section: Preferences & Display
        item {
            SettingsSectionHeader(title = "DISPLAY & PREFERENCES")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Dark mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (isDarkMode) Indigo500 else StatusAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text("Dark Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                Text("Indigo / Slate dark palette", fontSize = 11.sp, color = colors.textMuted)
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            modifier = Modifier.testTag("dark_mode_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Indigo500,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.border
                            )
                        )
                    }

                    HorizontalDivider(color = colors.border)

                    // Battery optimization whitelist shortcut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = if (isBatteryOptimized) StatusEmerald else StatusAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text("Battery Exemption", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                Text(
                                    text = if (isBatteryOptimized) "App whitelisted from sleep" else "Whitelist recommended",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(PipraPayService.getBatteryOptimizationIntent(context))
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(if (isBatteryOptimized) "Review" else "Whitelist", fontSize = 11.sp, color = colors.textPrimary)
                        }
                    }
                }
            }
        }

        // Section: Danger Zone / Unpair Device
        item {
            SettingsSectionHeader(title = "DEVICE SESSION & UNPAIRING")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("danger_zone_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, GhostRoseBorder)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unpair Device",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRose
                            )
                            Text(
                                text = "Revoke token and disconnect from BizliPay merchant account",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }

                        Button(
                            onClick = { showUnpairDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusRose,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("unpair_device_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unpair", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation Modal for Unpair Device
    if (showUnpairDialog) {
        AlertDialog(
            onDismissRequest = { showUnpairDialog = false },
            containerColor = colors.surfaceCard,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRose)
                    Text("Unpair this Device?", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to unpair this device from BizliPay? All background syncing will stop and you will need a new 6-digit OTP code to pair again.",
                    color = colors.textMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnpairDialog = false
                        viewModel.unpairDevice {
                            Toast.makeText(context, "Device unpaired successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRose),
                    modifier = Modifier.testTag("confirm_unpair_button")
                ) {
                    Text("Confirm Unpair", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnpairDialog = false },
                    modifier = Modifier.testTag("cancel_unpair_button")
                ) {
                    Text("Cancel", color = colors.textPrimary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = PipraTheme.colors
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = colors.textSubtle,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}
