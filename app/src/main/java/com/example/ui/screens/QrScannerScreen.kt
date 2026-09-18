package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusSynced
import com.example.ui.viewmodel.MainViewModel
import org.json.JSONObject

@Composable
fun QrScannerScreen(
    viewModel: MainViewModel,
    onConfigApplied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var qrRawPayload by remember { mutableStateOf("") }
    var parsedServerUrl by remember { mutableStateOf<String?>(null) }
    var parsedApiKey by remember { mutableStateOf<String?>(null) }
    var parsedDeviceKey by remember { mutableStateOf<String?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    fun parsePayload(input: String) {
        qrRawPayload = input
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            parsedServerUrl = null
            parsedApiKey = null
            parsedDeviceKey = null
            parseError = null
            return
        }

        // 1. PipraPay Official Companion QR Format: <url>----<otp>
        if (trimmed.contains("----")) {
            val parts = trimmed.split("----")
            val url = parts[0].trim()
            val otp = parts.getOrNull(1)?.trim() ?: ""
            if (url.startsWith("http://") || url.startsWith("https://")) {
                parsedServerUrl = if (url.endsWith("/")) url else "$url/"
                parsedApiKey = otp
                parsedDeviceKey = "DEV-" + (1000..9999).random()
                parseError = null
                return
            }
        }

        // 2. JSON configuration format
        try {
            val json = JSONObject(trimmed)
            val serverUrl = json.optString("server_url", json.optString("serverUrl", json.optString("url", ""))).trim()
            val apiKey = json.optString("api_key", json.optString("apiKey", json.optString("otp", json.optString("token", "")))).trim()
            val deviceKey = json.optString("device_key", json.optString("deviceKey", json.optString("device_id", ""))).trim()

            if (serverUrl.isBlank() && apiKey.isBlank()) {
                parseError = "Invalid configuration: Missing server URL or OTP in QR payload"
                parsedServerUrl = null
                parsedApiKey = null
                parsedDeviceKey = null
            } else {
                parsedServerUrl = serverUrl.ifBlank { "https://pay.emon.bd/" }
                parsedApiKey = apiKey
                parsedDeviceKey = deviceKey.ifBlank { "DEV-" + (1000..9999).random() }
                parseError = null
            }
            return
        } catch (_: Exception) {
            // Not JSON
        }

        // 3. Multi-line format: Line 1 = URL, Line 2 = OTP
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size >= 2 && (lines[0].startsWith("http://") || lines[0].startsWith("https://"))) {
            parsedServerUrl = lines[0]
            parsedApiKey = lines[1]
            parsedDeviceKey = lines.getOrNull(2) ?: ("DEV-" + (1000..9999).random())
            parseError = null
            return
        }

        // 4. Single OTP code
        if (trimmed.all { it.isDigit() } && trimmed.length in 6..12) {
            parsedServerUrl = "https://pay.emon.bd/"
            parsedApiKey = trimmed
            parsedDeviceKey = "DEV-" + (1000..9999).random()
            parseError = null
            return
        }

        parseError = "Unrecognized QR format. Expected PipraPay QR code (URL----OTP) or JSON."
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
        // Hero Card: QR Pairing Viewfinder
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_viewfinder_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "QR Scanner",
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Scan Merchant QR Configuration",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "From your PipraPay Merchant Dashboard, go to 'Pair Companion App', then paste or scan the generated configuration JSON below.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Payload Input Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_payload_input_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            text = "Configuration JSON",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clipText.isNullOrBlank()) {
                                    parsePayload(clipText)
                                    Toast.makeText(context, "Pasted from clipboard!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Clipboard")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                parsePayload("https://pay.emon.bd/----0702746925")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("pay.emon.bd QR", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                parsePayload("{\n  \"server_url\": \"https://pay.emon.bd/\",\n  \"api_key\": \"0702746925\"\n}")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Sample JSON", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = qrRawPayload,
                        onValueChange = { parsePayload(it) },
                        placeholder = {
                            Text(
                                "https://pay.emon.bd/----0702746925\n\nor JSON:\n{\n  \"server_url\": \"https://pay.emon.bd/\",\n  \"api_key\": \"0702746925\"\n}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("qr_json_text_field"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Parsed Configuration Preview
        parsedServerUrl?.let { server ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parsed_config_preview_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusSynced,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Valid Configuration Detected",
                                fontWeight = FontWeight.Bold,
                                color = StatusSynced,
                                fontSize = 14.sp
                            )
                        }

                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Server URL:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(server, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("API Key:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (parsedApiKey.isNullOrBlank()) "None" else "${parsedApiKey!!.take(6)}••••••••",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Device Key:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                parsedDeviceKey ?: "Default",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

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
                                .height(48.dp)
                                .testTag("apply_qr_config_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Pair and Connect Device", fontWeight = FontWeight.Bold)
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
                    color = StatusFailed.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusFailed,
                            modifier = Modifier.size(20.dp)
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
}
