package com.example.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.PipraPayService
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandIndigoLight
import com.example.ui.theme.BrandIndigoRing
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.InputBackgroundLight
import com.example.ui.theme.InputBorderLight
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSynced
import com.example.ui.viewmodel.LoginState
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class OnboardingStep {
    SYSTEM_READINESS,
    PERMISSIONS_SETUP,
    PANEL_LOGIN,
    QR_SCAN
}

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.SYSTEM_READINESS) }

    BackHandler(enabled = currentStep != OnboardingStep.SYSTEM_READINESS) {
        currentStep = when (currentStep) {
            OnboardingStep.QR_SCAN -> OnboardingStep.PANEL_LOGIN
            OnboardingStep.PANEL_LOGIN -> OnboardingStep.PERMISSIONS_SETUP
            OnboardingStep.PERMISSIONS_SETUP -> OnboardingStep.SYSTEM_READINESS
            OnboardingStep.SYSTEM_READINESS -> OnboardingStep.SYSTEM_READINESS
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("onboarding_screen")
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "OnboardingStepAnimation"
        ) { step ->
            when (step) {
                OnboardingStep.SYSTEM_READINESS -> {
                    SystemReadinessStep(
                        onContinue = { currentStep = OnboardingStep.PERMISSIONS_SETUP }
                    )
                }
                OnboardingStep.PERMISSIONS_SETUP -> {
                    PermissionsSetupStep(
                        viewModel = viewModel,
                        onBack = { currentStep = OnboardingStep.SYSTEM_READINESS },
                        onContinue = { currentStep = OnboardingStep.PANEL_LOGIN }
                    )
                }
                OnboardingStep.PANEL_LOGIN -> {
                    LoginScreen(
                        viewModel = viewModel,
                        onBack = { currentStep = OnboardingStep.PERMISSIONS_SETUP },
                        onNavigateToQr = { currentStep = OnboardingStep.QR_SCAN },
                        onLoginSuccess = onOnboardingFinished
                    )
                }
                OnboardingStep.QR_SCAN -> {
                    QrScannerScreen(
                        viewModel = viewModel,
                        onConfigApplied = onOnboardingFinished,
                        onNavigateToSettings = { currentStep = OnboardingStep.PANEL_LOGIN }
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 1: SYSTEM READINESS ("Get Ready")
// ==========================================
@Composable
private fun SystemReadinessStep(
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Step indicator badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BrandIndigo.copy(alpha = 0.10f),
            modifier = Modifier.testTag("onboarding_step_badge")
        ) {
            Text(
                text = "STEP 1 OF 3 • GETTING READY",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandIndigo,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Logo & Title
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BrandIndigo)
                .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = BrandIndigo),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = "PipraPay Logo",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Welcome to PipraPay",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Preparing your phone as an automated 24/7 MFS Payment Gateway Companion for bKash, Nagad, Rocket & Upay.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Diagnostics Readiness Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("readiness_checklist_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "System Diagnostics & Setup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                DiagnosticItem(
                    icon = Icons.Default.Sms,
                    title = "SMS Telephony Subsystem",
                    subtitle = "Hardware receiver ready for real-time MFS parsing"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                DiagnosticItem(
                    icon = Icons.Default.Security,
                    title = "Encrypted Vault (AES-256 GCM)",
                    subtitle = "Hardware-backed secure preferences active"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                DiagnosticItem(
                    icon = Icons.Default.Speed,
                    title = "MFS Regex Parser Engine",
                    subtitle = "bKash (16247), Nagad (16167), Rocket, Upay loaded"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                DiagnosticItem(
                    icon = Icons.Default.Storage,
                    title = "Local Cache & Sync Queue",
                    subtitle = "Offline-first SQLite Room database initialized"
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(32.dp))

        // Continue Button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("continue_to_permissions_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text(
                text = "Continue to Permissions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DiagnosticItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(StatusSynced.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StatusSynced,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(StatusSynced),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Ready",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ==========================================
// STEP 2: NECESSARY PERMISSIONS
// ==========================================
@Composable
private fun PermissionsSetupStep(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()

    fun checkNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

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
        mutableStateOf(checkNotificationGranted())
    }

    fun updatePermissionsState() {
        hasSmsReceive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        hasSmsRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        hasNotificationPermission = checkNotificationGranted()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        updatePermissionsState()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updatePermissionsState()
                viewModel.refreshBatteryOptimizationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        updatePermissionsState()
        viewModel.refreshBatteryOptimizationStatus()
    }

    val smsGranted = hasSmsReceive && hasSmsRead
    val allRequiredGranted = smsGranted && hasNotificationPermission

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("permissions_setup_screen"),
        horizontalAlignment = Alignment.Start
    ) {
        // Top Bar with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBack() }
                    .border(1.dp, InputBorderLight, RoundedCornerShape(12.dp))
                    .testTag("permissions_back_button"),
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

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BrandIndigo.copy(alpha = 0.10f)
            ) {
                Text(
                    text = "STEP 2 OF 3",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandIndigo
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Required Permissions",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "To monitor incoming customer payments and forward them to your merchant panel seamlessly, PipraPay needs access to SMS alerts and background services.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. SMS Interception Permission Card
        PermissionItemCard(
            title = "SMS Payment Detection",
            tag = "SMS",
            description = "Intercepts and parses transaction TrxID, sender phone, and amount from bKash, Nagad, Rocket, and Upay.",
            isGranted = smsGranted,
            onActionClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Notification Permission Card
        PermissionItemCard(
            title = "Service Notifications",
            tag = "NOTIFICATIONS",
            description = "Shows an ongoing status pill and alerts you when transactions are synced with the payment panel.",
            isGranted = hasNotificationPermission,
            onActionClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                } else {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Please enable notifications in system settings", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Battery Optimization Exemption Card
        PermissionItemCard(
            title = "Background Battery Exemption",
            tag = "BATTERY",
            description = "Crucial for MIUI, Samsung, Oppo & Vivo devices to prevent the OS from killing the SMS listener when the screen is locked.",
            isGranted = isBatteryOptimized,
            isExemption = true,
            onActionClick = {
                try {
                    context.startActivity(PipraPayService.getBatteryOptimizationIntent(context))
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open battery settings directly: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Continue to Login Screen Button
        Button(
            onClick = {
                if (allRequiredGranted) {
                    onContinue()
                }
            },
            enabled = allRequiredGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("proceed_to_login_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandIndigo,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(26.dp)
        ) {
            Text(
                text = "Continue to Panel Login",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (!allRequiredGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please grant all required permissions above to continue.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    tag: String,
    description: String,
    isGranted: Boolean,
    isExemption: Boolean = false,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_item_$tag"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGranted) StatusSynced.copy(alpha = 0.12f) else StatusPending.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isGranted) StatusSynced else StatusPending,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isGranted) (if (isExemption) "EXEMPTED" else "GRANTED") else (if (isExemption) "RECOMMENDED" else "REQUIRED"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) StatusSynced else StatusPending
                        )
                    }
                }
            }

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            if (!isGranted) {
                Button(
                    onClick = onActionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExemption) StatusPending else BrandIndigo
                    )
                ) {
                    Text(
                        text = if (isExemption) "Request Battery Exemption" else "Grant Permission",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 3: FINAL LOGIN SCREEN
// (Faithfully styled according to user's uploaded screenshot)
// ==========================================
@Composable
private fun PanelLoginStep(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    LoginScreen(
        viewModel = viewModel,
        onBack = onBack,
        onLoginSuccess = onLoginSuccess
    )
}

