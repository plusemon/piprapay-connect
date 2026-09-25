package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BizliPayIcon
import com.example.ui.theme.Cyan500
import com.example.ui.theme.GhostIndigoBg
import com.example.ui.theme.GhostIndigoBorder
import com.example.ui.theme.GhostRoseBg
import com.example.ui.theme.GhostRoseBorder
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.PipraTheme
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.viewmodel.LoginState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    onNavigateToQr: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = PipraTheme.colors

    val isLoading = loginState is LoginState.Loading

    var panelUrl by remember(settings.serverBaseUrl) {
        mutableStateOf(if (settings.serverBaseUrl.isNotBlank()) settings.serverBaseUrl else "https://bizlipay.com")
    }
    var otpCode by remember(settings.otp) {
        mutableStateOf(settings.otp)
    }

    var urlError by remember { mutableStateOf<String?>(null) }
    var otpError by remember { mutableStateOf<String?>(null) }

    // Device telemetry details for auto-detection card
    val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
    val model = Build.MODEL.orEmpty()
    val fullDeviceModel = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    val androidLevel = "API ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})"
    val appVersion = "v1.0.0"

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val successMsg = (loginState as LoginState.Success).message
            Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
            viewModel.resetLoginState()
            onLoginSuccess()
        }
    }

    fun executePairing() {
        if (isLoading) return
        val trimmedUrl = panelUrl.trim()
        val trimmedOtp = otpCode.trim()

        var hasError = false
        if (trimmedUrl.isBlank()) {
            urlError = "Server URL cannot be empty"
            hasError = true
        } else {
            urlError = null
        }

        if (trimmedOtp.isBlank()) {
            otpError = "Please enter the 6-digit OTP code"
            hasError = true
        } else {
            otpError = null
        }

        if (hasError) {
            viewModel.setLoginError("Please provide both Server URL and 6-digit OTP pairing code.")
            return
        }

        viewModel.pairDevice(
            serverUrl = trimmedUrl,
            otp = trimmedOtp,
            onSuccess = onLoginSuccess
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("login_account_screen")
            .testTag("pair_device_screen"),
        horizontalAlignment = Alignment.Start
    ) {
        // Back Button
        if (onBack != null) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoading) { onBack() }
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .testTag("login_back_button"),
                color = colors.surfaceCard,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Header Logo & Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BizliPayIcon(size = 44.dp)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GhostIndigoBg,
                border = BorderStroke(1.dp, GhostIndigoBorder)
            ) {
                Text(
                    text = "BIZLIPAY CONNECT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo500,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Screen Title: "Pair Device"
        Text(
            text = "Pair Device",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.testTag("login_screen_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle: "Connect this device to your BizliPay Merchant Account"
        Text(
            text = "Connect this device to your BizliPay Merchant Account",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Field 1: Payment Panel / Server URL
        Text(
            text = "Payment Panel / Server URL",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = panelUrl,
            onValueChange = {
                panelUrl = it
                if (it.isNotBlank()) urlError = null
            },
            enabled = !isLoading,
            placeholder = {
                Text(
                    text = "https://bizlipay.com",
                    color = colors.textSubtle,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            isError = urlError != null,
            supportingText = if (urlError != null) {
                { Text(text = urlError ?: "", color = StatusRose, fontSize = 12.sp) }
            } else null,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payment_panel_url_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceCard,
                unfocusedContainerColor = colors.surfaceCard,
                focusedBorderColor = Indigo500,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = Indigo500
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Field 2: One Time Password / OTP (6-digit pairing code)
        Text(
            text = "One Time Password / Pairing Code",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = {
                otpCode = it
                if (it.isNotBlank()) otpError = null
            },
            enabled = !isLoading,
            placeholder = {
                Text(
                    text = "e.g. 123456",
                    color = colors.textSubtle,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = otpError != null,
            supportingText = if (otpError != null) {
                { Text(text = otpError ?: "", color = StatusRose, fontSize = 12.sp) }
            } else null,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = colors.textPrimary,
                letterSpacing = 2.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("one_time_password_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceCard,
                unfocusedContainerColor = colors.surfaceCard,
                focusedBorderColor = Indigo500,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = Indigo500
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Helper text: "Generate your 6-digit code in Merchant Dashboard > Devices > Add Device."
        Text(
            text = "Generate your 6-digit code in Merchant Dashboard > Devices > Add Device.",
            fontSize = 12.sp,
            color = colors.textMuted,
            lineHeight = 16.sp,
            modifier = Modifier.testTag("otp_helper_text")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Device Info Card: Auto-detect and display current device model, Android OS level, and app version
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("device_info_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = Cyan500,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "DEVICE TELEMETRY SPECIFICATIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSubtle,
                        letterSpacing = 0.8.sp
                    )
                }

                HorizontalDivider(color = colors.border, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Model", fontSize = 12.sp, color = colors.textMuted)
                    Text(text = fullDeviceModel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Android OS", fontSize = 12.sp, color = colors.textMuted)
                    Text(text = androidLevel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "App Version", fontSize = 12.sp, color = colors.textMuted)
                    Text(text = appVersion, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Cyan500)
                }
            }
        }

        // Error Banner
        AnimatedVisibility(visible = loginState is LoginState.Error) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("login_error_banner"),
                shape = RoundedCornerShape(10.dp),
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
                        tint = StatusRose,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = (loginState as? LoginState.Error)?.message ?: "Pairing failed",
                        fontSize = 12.sp,
                        color = StatusRose,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Button: "Pair Device"
        Button(
            onClick = { executePairing() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("login_button")
                .testTag("pair_device_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Indigo600,
                contentColor = Color.White,
                disabledContainerColor = colors.border,
                disabledContentColor = colors.textMuted
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Pairing Device...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Pair Device",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Divider OR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
            Text(
                text = "OR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSubtle,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QR Code alternative action
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceCard)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(enabled = !isLoading) { onNavigateToQr?.invoke() }
                    .testTag("qr_code_login_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Pair via QR Code",
                    tint = Cyan500,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Or pair with QR code",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                modifier = Modifier
                    .clickable(enabled = !isLoading) { onNavigateToQr?.invoke() }
                    .testTag("or_login_with_qr_code_text")
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
