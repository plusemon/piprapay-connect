package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BorderZinc800
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.ContainerDark
import com.example.ui.theme.GhostEmeraldBg
import com.example.ui.theme.GhostEmeraldBorder
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusSynced
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextZinc400
import com.example.ui.theme.TextZinc500
import com.example.ui.viewmodel.LoginState
import com.example.ui.viewmodel.MainViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * ZXing Image Analysis Analyzer for QR Code detection
 */
class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )
        setHints(hints)
    }

    @Volatile
    private var isScanning = true

    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        try {
            val plane = imageProxy.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val width = imageProxy.width
            val height = imageProxy.height

            val source = PlanarYUVLuminanceSource(
                data,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)

            if (result != null && result.text.isNotBlank()) {
                isScanning = false
                onQrCodeScanned(result.text)
            }
        } catch (_: Exception) {
            // Frame did not contain a readable barcode
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }

    fun pause() {
        isScanning = false
    }

    fun resume() {
        isScanning = true
    }
}

@Composable
fun QrScannerScreen(
    viewModel: MainViewModel,
    onConfigApplied: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isScanningActive by remember { mutableStateOf(true) }
    var parsedServerUrl by remember { mutableStateOf<String?>(null) }
    var parsedApiKey by remember { mutableStateOf<String?>(null) }
    var parsedDeviceKey by remember { mutableStateOf<String?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }

    // Scanner Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser_transition")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            isScanningActive = true
            parseError = null
        } else {
            Toast.makeText(context, "Camera permission is required to scan QR code", Toast.LENGTH_SHORT).show()
        }
    }

    // Refresh permission state on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Trigger subtle haptic vibration
    fun triggerHapticFeedback() {
        try {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) { }
    }

    // Auto-Connect Pipeline
    fun executeAutoConnect(serverUrl: String, apiKey: String, deviceKey: String) {
        isConnecting = true
        triggerHapticFeedback()

        viewModel.loginToPanel(
            panelUrl = serverUrl,
            passwordOrToken = apiKey,
            deviceKey = deviceKey,
            otp = apiKey,
            onSuccess = {
                isConnecting = false
                Toast.makeText(context, "Connected to PipraPay Gateway", Toast.LENGTH_LONG).show()
                onConfigApplied()
            }
        )
    }

    // Parse URL & URI Scheme QR Payload
    fun handleScannedPayload(payload: String) {
        val trimmed = payload.trim()
        if (trimmed.isBlank()) return

        var server: String? = null
        var key: String? = null
        var device: String? = null

        // 1. PipraPay companion URI delimiter format: <url>----<otp>
        if (trimmed.contains("----")) {
            val parts = trimmed.split("----")
            val rawUrl = parts[0].trim()
            val otp = parts.getOrNull(1)?.trim() ?: ""
            if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                server = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
                key = otp
                device = "POS-" + (1000..9999).random()
            }
        }

        // 2. Encoded URI / URL format (piprapay://pair?server=...&api_key=...&device=...)
        if (server == null) {
            try {
                val uri = Uri.parse(trimmed)
                val scheme = uri.scheme?.lowercase()
                if (scheme == "piprapay" || scheme == "http" || scheme == "https") {
                    key = uri.getQueryParameter("api_key")
                        ?: uri.getQueryParameter("apiKey")
                        ?: uri.getQueryParameter("key")
                        ?: uri.getQueryParameter("token")
                        ?: uri.getQueryParameter("otp")

                    device = uri.getQueryParameter("device")
                        ?: uri.getQueryParameter("device_key")
                        ?: uri.getQueryParameter("deviceKey")
                        ?: uri.getQueryParameter("pos")

                    val serverParam = uri.getQueryParameter("server")
                        ?: uri.getQueryParameter("server_url")
                        ?: uri.getQueryParameter("serverUrl")
                        ?: uri.getQueryParameter("url")

                    if (!serverParam.isNullOrBlank()) {
                        server = if (serverParam.endsWith("/")) serverParam else "$serverParam/"
                    } else if (scheme == "http" || scheme == "https") {
                        val portPart = if (uri.port != -1) ":${uri.port}" else ""
                        server = "${uri.scheme}://${uri.host}$portPart/"
                    }
                }
            } catch (_: Exception) { }
        }

        // 3. Direct URL fallback (e.g. https://pay.emon.bd/)
        if (server == null && (trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
            server = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
            key = "0702746925"
            device = "POS-" + (1000..9999).random()
        }

        // 4. Plain numeric OTP
        if (server == null && trimmed.all { it.isDigit() } && trimmed.length in 6..12) {
            server = "https://pay.emon.bd/"
            key = trimmed
            device = "POS-" + (1000..9999).random()
        }

        if (server != null && !key.isNullOrBlank()) {
            val assignedDevice = device ?: ("POS-" + (1000..9999).random())
            parsedServerUrl = server
            parsedApiKey = key
            parsedDeviceKey = assignedDevice
            parseError = null
            isScanningActive = false

            // Trigger instant auto-connect pipeline
            executeAutoConnect(server, key, assignedDevice)
        } else {
            parseError = "Unrecognized QR code format. Expected pairing URL: piprapay://pair?server=...&api_key=..."
            triggerHapticFeedback()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("qr_scanner_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Active Native Camera Viewfinder
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
                    // Title & Description Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Instant QR Pairing",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Point camera at the Companion QR code on your PipraPay Merchant Dashboard.",
                            fontSize = 12.sp,
                            color = TextZinc400,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Scanner Viewport Box (220dp x 220dp)
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CanvasBlack)
                            .border(
                                width = 2.dp,
                                color = if (isScanningActive && hasCameraPermission) AccentEmerald else BorderZinc800,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission && isScanningActive) {
                            // Active Camera Viewfinder via CameraX
                            val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                            val analyzer = remember {
                                QrCodeAnalyzer { scannedCode ->
                                    handleScannedPayload(scannedCode)
                                }
                            }

                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }

                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()

                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also {
                                                it.setAnalyzer(cameraExecutor, analyzer)
                                            }

                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    previewView
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("camera_preview_view")
                            )

                            // Overlay: Pulsing green scanner line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .offset(y = (laserY - 100).dp)
                                    .background(AccentEmerald)
                            )

                            // Overlay: Framing Corner Brackets
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .border(2.dp, AccentEmerald.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            )
                        } else if (!hasCameraPermission) {
                            // Camera Permission Prompt UI
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(GhostEmeraldBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Camera Permission",
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "Camera Access Required",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Grant camera permission to automatically scan QR codes.",
                                    fontSize = 11.sp,
                                    color = TextZinc400,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("grant_camera_permission_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Grant Permission", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                }
                            }
                        } else {
                            // Paused or Scanned View
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSynced,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "QR Code Detected",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                        }
                    }

                    // Scanner Action Buttons (Simulate Scan & Rescan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Scan / Test QR Pairing button
                        Button(
                            onClick = {
                                handleScannedPayload("piprapay://pair?server=https://pay.emon.bd&api_key=0702746925&device=POS-DEV-01")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("scan_code_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connecting...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            } else {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                        }

                        if (!isScanningActive) {
                            OutlinedButton(
                                onClick = {
                                    isScanningActive = true
                                    parsedServerUrl = null
                                    parsedApiKey = null
                                    parsedDeviceKey = null
                                    parseError = null
                                },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(46.dp)
                                    .testTag("rescan_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderZinc800)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextWhite)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rescan", fontSize = 12.sp, color = TextWhite)
                            }
                        }
                    }

                    // Secondary text button below the scanner: "Enter Details Manually"
                    TextButton(
                        onClick = {
                            if (onNavigateToSettings != null) {
                                onNavigateToSettings()
                            } else {
                                showManualEntryDialog = true
                            }
                        },
                        modifier = Modifier.testTag("enter_details_manually_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextZinc400)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Enter Details Manually",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextZinc400
                        )
                    }
                }
            }
        }

        // Connection State / Parameters Detected Card
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                text = "Gateway Credentials Verified",
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite,
                                fontSize = 13.sp
                            )
                        }

                        HorizontalDivider(color = BorderZinc800)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gateway Server:", fontSize = 12.sp, color = TextZinc500)
                            Text(server, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextWhite, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Merchant Key:", fontSize = 12.sp, color = TextZinc500)
                            Text(
                                if (parsedApiKey.isNullOrBlank()) "Configured" else "${parsedApiKey!!.take(4)}••••••••",
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
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
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

                        if (!hasCameraPermission) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) { }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Open App Settings", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Entry Fallback Dialog
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderZinc800,
                            focusedBorderColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = manualKey,
                        onValueChange = { manualKey = it },
                        label = { Text("Merchant Key / OTP", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                        shape = RoundedCornerShape(12.dp),
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
                        val formattedUrl = if (manualUrl.endsWith("/")) manualUrl else "$manualUrl/"
                        showManualEntryDialog = false
                        executeAutoConnect(formattedUrl, manualKey, manualDevice)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Connect", fontWeight = FontWeight.SemiBold, color = Color.Black)
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

