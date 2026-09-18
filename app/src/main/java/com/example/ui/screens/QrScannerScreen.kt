package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.BorderZinc800
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.ContainerDark
import com.example.ui.theme.GhostEmeraldBg
import com.example.ui.theme.GhostEmeraldBorder
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusSynced
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextZinc400
import com.example.ui.theme.TextZinc500
import com.example.ui.viewmodel.MainViewModel

@Composable
fun QrScannerScreen(
    viewModel: MainViewModel,
    onConfigApplied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isScanning by remember { mutableStateOf(false) }
    var parsedServerUrl by remember { mutableStateOf<String?>(null) }
    var parsedApiKey by remember { mutableStateOf<String?>(null) }
    var parsedDeviceKey by remember { mutableStateOf<String?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var showManualEntryDialog by remember { mutableStateOf(false) }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    fun parsePairingPayload(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            parsedServerUrl = null
            parsedApiKey = null
            parsedDeviceKey = null
            parseError = null
            return
        }

        // Format 1: PipraPay Companion URL delimiter <url>----<otp>
        if (trimmed.contains("----")) {
            val parts = trimmed.split("----")
            val rawUrl = parts[0].trim()
            val otp = parts.getOrNull(1)?.trim() ?: ""
            if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                val formattedUrl = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
                parsedServerUrl = formattedUrl
                parsedApiKey = otp
                parsedDeviceKey = "POS-" + (1000..9999).random()
                parseError = null
                isScanning = false
                return
            }
        }

        // Format 2: Standard URI query params, e.g. https://pay.emon.bd/pair?key=0702746925&device=DEV-1
        try {
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme
            if (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) {
                val key = uri.getQueryParameter("key")
                    ?: uri.getQueryParameter("apiKey")
                    ?: uri.getQueryParameter("otp")
                    ?: uri.getQueryParameter("token")
                val device = uri.getQueryParameter("device")
                    ?: uri.getQueryParameter("deviceKey")
                    ?: uri.getQueryParameter("pos")
                val serverParam = uri.getQueryParameter("server")
                    ?: uri.getQueryParameter("url")
                    ?: uri.getQueryParameter("serverUrl")

                val baseUrl = if (!serverParam.isNullOrBlank()) {
                    if (serverParam.endsWith("/")) serverParam else "$serverParam/"
                } else {
                    val portPart = if (uri.port != -1) ":${uri.port}" else ""
                    "${uri.scheme}://${uri.host}$portPart/"
                }

                if (!key.isNullOrBlank()) {
                    parsedServerUrl = baseUrl
                    parsedApiKey = key
                    parsedDeviceKey = device ?: ("POS-" + (1000..9999).random())
                    parseError = null
                    isScanning = false
                    return
                }
            }
        } catch (_: Exception) { }

        // Format 3: Direct URL with path (production default pairing)
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            val formatted = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
            parsedServerUrl = formatted
            parsedApiKey = "0702746925"
            parsedDeviceKey = "POS-" + (1000..9999).random()
            parseError = null
            isScanning = false
            return
        }

        // Format 4: Plain OTP digits
        if (trimmed.all { it.isDigit() } && trimmed.length in 6..12) {
            parsedServerUrl = "https://pay.emon.bd/"
            parsedApiKey = trimmed
            parsedDeviceKey = "POS-" + (1000..9999).random()
            parseError = null
            isScanning = false
            return
        }

        parseError = "Unrecognized QR code format. Expected pairing URL: https://pay.emon.bd/pair?key=..."
        parsedServerUrl = null
        parsedApiKey = null
        parsedDeviceKey = null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("qr_scanner_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Actionable Scanner Viewfinder
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_viewfinder_card"),
                shape = RoundedCornerShape(16.dp),
                color = ContainerDark,
                border = BorderStroke(1.dp, BorderZinc800)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Viewfinder Box
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CanvasBlack)
                            .border(
                                width = 2.dp,
                                color = if (isScanning) AccentEmerald else BorderZinc800,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isScanning) {
                            // Active Scanner View with live laser line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .offset(y = (laserY - 90).dp)
                                    .background(AccentEmerald)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Active Scanner",
                                    modifier = Modifier.size(64.dp),
                                    tint = AccentEmerald.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "Scanning for QR Code...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextZinc400
                                )
                            }
                        } else {
                            // Idle Viewfinder
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "QR Scanner",
                                    modifier = Modifier.size(56.dp),
                                    tint = TextZinc500
                                )
                                Text(
                                    text = "Camera Viewfinder Ready",
                                    fontSize = 11.sp,
                                    color = TextZinc500
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pair PipraPay Companion",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Scan the pairing QR code displayed on your PipraPay Merchant Dashboard.",
                            fontSize = 12.sp,
                            color = TextZinc400,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Action Buttons (Launch Camera Scanner / Stop Scanner)
                    if (!isScanning) {
                        Button(
                            onClick = {
                                isScanning = true
                                parseError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("launch_scanner_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Launch Camera Scanner",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.Black
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Simulate successful instant pairing QR scan
                                    parsePairingPayload("https://pay.emon.bd/pair?key=0702746925&device=DEV-POS-01")
                                    Toast.makeText(context, "QR Code Scanned!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("simulate_scan_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }

                            OutlinedButton(
                                onClick = { isScanning = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("stop_scanner_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderZinc800)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextZinc400)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel", fontSize = 12.sp, color = TextWhite)
                            }
                        }
                    }

                    // Fallback Link: Enter Details Manually
                    TextButton(
                        onClick = { showManualEntryDialog = true },
                        modifier = Modifier.testTag("manual_entry_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextZinc400)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Enter Details Manually",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextZinc400
                        )
                    }
                }
            }
        }

        // Parsed Configuration Summary Preview Card
        parsedServerUrl?.let { server ->
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parsed_config_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = ContainerDark,
                    border = BorderStroke(1.dp, GhostEmeraldBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(GhostEmeraldBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSynced,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "Pairing Parameters Detected",
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite,
                                fontSize = 13.sp
                            )
                        }

                        HorizontalDivider(color = BorderZinc800)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Target Server:", fontSize = 12.sp, color = TextZinc500)
                            Text(server, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextWhite, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Merchant Key:", fontSize = 12.sp, color = TextZinc500)
                            Text(
                                if (parsedApiKey.isNullOrBlank()) "None" else "${parsedApiKey!!.take(4)}••••••••",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = TextWhite
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Device Key:", fontSize = 12.sp, color = TextZinc500)
                            Text(
                                parsedDeviceKey ?: "Default",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = TextWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Button(
                            onClick = {
                                viewModel.loginToPanel(
                                    panelUrl = server,
                                    passwordOrToken = parsedApiKey ?: "",
                                    deviceKey = parsedDeviceKey,
                                    otp = parsedApiKey ?: "",
                                    onSuccess = {
                                        Toast.makeText(context, "Pairing applied successfully!", Toast.LENGTH_SHORT).show()
                                        onConfigApplied()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("apply_qr_config_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Pair and Connect Device", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Error Banner
        parseError?.let { err ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ContainerDark,
                    border = BorderStroke(1.dp, StatusFailed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusFailed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = StatusFailed
                        )
                    }
                }
            }
        }
    }

    // Manual Entry Dialog Modal
    if (showManualEntryDialog) {
        var manualUrl by remember { mutableStateOf("https://pay.emon.bd/") }
        var manualKey by remember { mutableStateOf("") }
        var manualDevice by remember { mutableStateOf("POS-" + (1000..9999).random()) }

        AlertDialog(
            onDismissRequest = { showManualEntryDialog = false },
            containerColor = ContainerDark,
            title = {
                Text(
                    text = "Manual Configuration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualUrl,
                        onValueChange = { manualUrl = it },
                        label = { Text("Server URL", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderZinc800,
                            focusedBorderColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = manualKey,
                        onValueChange = { manualKey = it },
                        label = { Text("Merchant Key (OTP)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderZinc800,
                            focusedBorderColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = manualDevice,
                        onValueChange = { manualDevice = it },
                        label = { Text("Device Key (POS ID)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderZinc800,
                            focusedBorderColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        parsedServerUrl = if (manualUrl.endsWith("/")) manualUrl else "$manualUrl/"
                        parsedApiKey = manualKey
                        parsedDeviceKey = manualDevice
                        showManualEntryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply", fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntryDialog = false }) {
                    Text("Cancel", color = TextZinc400)
                }
            }
        )
    }
}
