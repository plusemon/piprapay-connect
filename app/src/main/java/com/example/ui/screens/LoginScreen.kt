package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandIndigoRing
import com.example.ui.theme.InputBackgroundLight
import com.example.ui.theme.InputBorderLight
import com.example.ui.theme.StatusFailed
import com.example.ui.viewmodel.LoginState
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var panelUrl by remember(settings.serverBaseUrl) {
        mutableStateOf(if (settings.serverBaseUrl.isNotBlank()) settings.serverBaseUrl else "https://api.piprapay.com")
    }
    var password by remember(settings.apiKey) {
        mutableStateOf(settings.apiKey)
    }

    var urlError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var showQrModal by remember { mutableStateOf(false) }
    var qrRawPayload by remember { mutableStateOf("") }
    var qrParseError by remember { mutableStateOf<String?>(null) }

    // Handle login state changes
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val successMsg = (loginState as LoginState.Success).message
            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
            viewModel.resetLoginState()
            onLoginSuccess()
        }
    }

    // Core validation and login execution
    fun validateAndExecuteLogin() {
        val trimmedUrl = panelUrl.trim()
        val trimmedPassword = password.trim()

        var hasError = false

        if (trimmedUrl.isBlank()) {
            urlError = "Payment Panel URL cannot be empty"
            hasError = true
        } else {
            urlError = null
        }

        if (trimmedPassword.isBlank()) {
            passwordError = "One Time Password cannot be empty"
            hasError = true
        } else {
            passwordError = null
        }

        if (hasError) {
            val errorMsg = when {
                trimmedUrl.isBlank() && trimmedPassword.isBlank() -> "Payment Panel URL and credentials cannot be empty"
                trimmedUrl.isBlank() -> "Payment Panel URL cannot be empty"
                else -> "One Time Password cannot be empty"
            }
            viewModel.setLoginError(errorMsg)
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            return
        }

        // Validation passed: save credentials to EncryptedSharedPreferences, set isOnboardingCompleted = true,
        // start PipraPayForegroundService, and trigger navigation to DashboardScreen
        viewModel.loginToPanel(
            panelUrl = trimmedUrl,
            passwordOrToken = trimmedPassword,
            onSuccess = {
                onLoginSuccess()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("login_account_screen"),
        horizontalAlignment = Alignment.Start
    ) {
        // Top Back Button (if provided)
        if (onBack != null) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBack() }
                    .border(1.dp, InputBorderLight, RoundedCornerShape(12.dp))
                    .testTag("login_back_button"),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Main Title
        Text(
            text = "Login your account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("login_screen_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle
        Text(
            text = "Welcome back, Sign in to your account",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Label: Payment Panel URL
        Text(
            text = "Payment Panel URL",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input: Payment Panel URL
        OutlinedTextField(
            value = panelUrl,
            onValueChange = {
                panelUrl = it
                if (it.isNotBlank()) urlError = null
            },
            placeholder = {
                Text(
                    text = "https://api.piprapay.com",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            },
            isError = urlError != null,
            supportingText = if (urlError != null) {
                { Text(text = urlError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payment_panel_url_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = InputBackgroundLight,
                focusedBorderColor = BrandIndigo,
                unfocusedBorderColor = InputBorderLight
            )
        )

        // Quick Preset URL Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = panelUrl == "https://api.piprapay.com",
                onClick = {
                    panelUrl = "https://api.piprapay.com"
                    urlError = null
                },
                label = { Text("Default Live", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandIndigo.copy(alpha = 0.12f),
                    selectedLabelColor = BrandIndigo
                )
            )
            FilterChip(
                selected = panelUrl == "https://staging-api.piprapay.com",
                onClick = {
                    panelUrl = "https://staging-api.piprapay.com"
                    urlError = null
                },
                label = { Text("Staging Server", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandIndigo.copy(alpha = 0.12f),
                    selectedLabelColor = BrandIndigo
                )
            )
            FilterChip(
                selected = panelUrl == "http://10.0.2.2:8080",
                onClick = {
                    panelUrl = "http://10.0.2.2:8080"
                    urlError = null
                },
                label = { Text("Localhost", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandIndigo.copy(alpha = 0.12f),
                    selectedLabelColor = BrandIndigo
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Label: One Time Password
        Text(
            text = "One Time Password",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input: One Time Password
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (it.isNotBlank()) passwordError = null
            },
            placeholder = {
                Text(
                    text = "Enter password or secret token",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            },
            isError = passwordError != null,
            supportingText = if (passwordError != null) {
                { Text(text = passwordError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("one_time_password_input"),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password visibility",
                        tint = Color(0xFF94A3B8)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = InputBackgroundLight,
                focusedBorderColor = BrandIndigo,
                unfocusedBorderColor = InputBorderLight
            )
        )


        // Login Error Banner if needed
        AnimatedVisibility(visible = loginState is LoginState.Error) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .testTag("login_error_banner"),
                shape = RoundedCornerShape(12.dp),
                color = StatusFailed.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusFailed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = (loginState as? LoginState.Error)?.message ?: "Login failed",
                        fontSize = 12.sp,
                        color = StatusFailed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main "Login" Button (Pill shaped, vibrant purple/indigo, matching screenshot)
        Button(
            onClick = {
                validateAndExecuteLogin()
            },
            enabled = loginState !is LoginState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(26.dp), spotColor = BrandIndigo)
                .testTag("login_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
            shape = RoundedCornerShape(26.dp)
        ) {
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Connecting to Panel...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(34.dp))

        // Divider: "----------------- OR -----------------"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = InputBorderLight
            )
            Text(
                text = "OR",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = InputBorderLight
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Circular QR Scanner Button and "Or log in with QR code" action
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(BrandIndigoRing.copy(alpha = 0.5f))
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, BrandIndigo, CircleShape)
                    .clickable {
                        if (panelUrl.trim().isNotBlank() && password.trim().isNotBlank()) {
                            validateAndExecuteLogin()
                        } else {
                            validateAndExecuteLogin()
                            showQrModal = true
                        }
                    }
                    .testTag("qr_code_login_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Log in with QR code",
                    tint = BrandIndigo,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-caption: "Or log in with QR code"
            Text(
                text = "Or log in with QR code",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable {
                        if (panelUrl.trim().isNotBlank() && password.trim().isNotBlank()) {
                            validateAndExecuteLogin()
                        } else {
                            validateAndExecuteLogin()
                            showQrModal = true
                        }
                    }
                    .testTag("or_login_with_qr_code_text")
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Modal Sheet for QR Pairing & Fast Connection
    if (showQrModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { showQrModal = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("qr_login_modal_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan or Paste QR Config",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { showQrModal = false } }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "In your PipraPay Merchant Dashboard, click 'Companion QR Code' to copy or scan the configuration token.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Input or Paste Box
                OutlinedTextField(
                    value = qrRawPayload,
                    onValueChange = {
                        qrRawPayload = it
                        qrParseError = null
                    },
                    placeholder = {
                        Text(
                            "{\n  \"server_url\": \"https://api.piprapay.com\",\n  \"api_key\": \"pipra_live_...\"\n}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("qr_modal_json_field"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )

                // Paste from clipboard button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                        if (!text.isNullOrBlank()) {
                            qrRawPayload = text
                            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste Clipboard", fontSize = 12.sp)
                }

                qrParseError?.let { err ->
                    Text(
                        text = err,
                        color = StatusFailed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Apply and connect button
                Button(
                    onClick = {
                        try {
                            val json = JSONObject(qrRawPayload)
                            val parsedUrl = json.optString("server_url", json.optString("serverUrl", json.optString("url", ""))).trim()
                            val parsedKey = json.optString(
                                "api_key",
                                json.optString(
                                    "apiKey",
                                    json.optString("otp", json.optString("password", json.optString("token", json.optString("key", ""))))
                                )
                            ).trim()
                            val parsedDevice = json.optString("device_key", json.optString("deviceKey", json.optString("device_id", json.optString("deviceId", "")))).trim()

                            if (parsedUrl.isBlank() && parsedKey.isBlank()) {
                                qrParseError = "Invalid QR config. Please provide server URL and OTP or API key."
                            } else if (parsedUrl.isBlank()) {
                                qrParseError = "Payment Panel URL cannot be empty in QR configuration."
                            } else if (parsedKey.isBlank()) {
                                qrParseError = "OTP or API Key cannot be empty in QR configuration."
                            } else {
                                panelUrl = parsedUrl
                                password = parsedKey
                                showQrModal = false
                                Toast.makeText(context, "Configuration loaded! Logging in...", Toast.LENGTH_SHORT).show()
                                viewModel.loginToPanel(
                                    panelUrl = panelUrl,
                                    passwordOrToken = password,
                                    deviceKey = parsedDevice.ifBlank { null },
                                    onSuccess = {
                                        onLoginSuccess()
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            qrParseError = "Malformed JSON: ${e.localizedMessage}"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apply_qr_and_login_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Apply & Login Now", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
