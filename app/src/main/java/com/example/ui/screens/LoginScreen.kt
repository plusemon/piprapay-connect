package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandIndigoRing
import com.example.ui.theme.GhostRoseBg
import com.example.ui.theme.GhostRoseBorder
import com.example.ui.theme.InputBackgroundLight
import com.example.ui.theme.InputBorderLight
import com.example.ui.theme.StatusFailed
import com.example.ui.viewmodel.LoginState
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    val isLoading = loginState is LoginState.Loading

    var panelUrl by remember(settings.serverBaseUrl) {
        mutableStateOf(if (settings.serverBaseUrl.isNotBlank()) settings.serverBaseUrl else "https://pay.emon.bd/")
    }
    var password by remember(settings.apiKey) {
        mutableStateOf(settings.apiKey)
    }

    var urlError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var isPasswordVisible by remember { mutableStateOf(false) }

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
        if (isLoading) return
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
                trimmedUrl.isBlank() && trimmedPassword.isBlank() -> "Endpoint not found: Check your Payment Panel URL."
                trimmedUrl.isBlank() -> "Endpoint not found: Check your Payment Panel URL."
                else -> "Authentication failed: Invalid Merchant API Key."
            }
            viewModel.setLoginError(errorMsg)
            return
        }

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
                    .clickable(enabled = !isLoading) { onBack() }
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
            enabled = !isLoading,
            placeholder = {
                Text(
                    text = "https://pay.emon.bd/",
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

        // Production Host Default Preset Chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = panelUrl.contains("pay.emon.bd"),
                enabled = !isLoading,
                onClick = {
                    panelUrl = "https://pay.emon.bd/"
                    urlError = null
                },
                label = { Text("pay.emon.bd (Production)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandIndigo.copy(alpha = 0.15f),
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
            enabled = !isLoading,
            placeholder = {
                Text(
                    text = "Enter OTP or secret token",
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
                IconButton(
                    onClick = { isPasswordVisible = !isPasswordVisible },
                    enabled = !isLoading
                ) {
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

        // Login Error Banner (bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-lg p-3 text-xs)
        AnimatedVisibility(visible = loginState is LoginState.Error) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("login_error_banner"),
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
                        text = (loginState as? LoginState.Error)?.message ?: "Authentication failed: Invalid Merchant API Key.",
                        fontSize = 12.sp,
                        color = Color(0xFFFB7185),
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main "Login" Button (Disabled during handshake, shows inline spinner + "Verifying handshake...")
        Button(
            onClick = {
                validateAndExecuteLogin()
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp), spotColor = BrandIndigo)
                .testTag("login_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF27272A),
                disabledContentColor = Color(0xFF71717A)
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
                    text = "Verifying handshake...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Login",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
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
                    .clickable(enabled = !isLoading) {
                        onNavigateToQr?.invoke()
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
                    .clickable(enabled = !isLoading) {
                        onNavigateToQr?.invoke()
                    }
                    .testTag("or_login_with_qr_code_text")
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

