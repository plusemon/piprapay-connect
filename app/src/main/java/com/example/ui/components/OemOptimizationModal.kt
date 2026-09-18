package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BorderZinc700
import com.example.ui.theme.BorderZinc800
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.ContainerDark
import com.example.ui.theme.PipraTheme
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextZinc300
import com.example.ui.theme.TextZinc400
import com.example.ui.theme.TextZinc500
import com.example.util.OemWorkaroundHelper

/**
 * OEM Battery Optimization & Background Persistence Workaround Modal (Feature 2)
 * Guides merchants on Xiaomi, Oppo, Vivo, Samsung to configure 24/7 background gateway uptime.
 */
@Composable
fun OemOptimizationModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val guidance = remember { OemWorkaroundHelper.getGuidance() }
    val colors = PipraTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .testTag("oem_workaround_modal"),
            shape = RoundedCornerShape(16.dp),
            color = colors.container,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccentAmber)
                            )
                            Text(
                                text = "BACKGROUND PERSISTENCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = AccentAmber
                            )
                        }

                        Text(
                            text = "Unrestricted Background Running Required",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            lineHeight = 22.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Detected Device Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceCard,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Detected Architecture",
                                fontSize = 10.sp,
                                color = colors.textSubtle,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${guidance.brand.displayName} (${guidance.brand.systemUiName})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                Text(
                    text = "Aggressive OEM battery killers can freeze incoming SMS listeners when the screen is locked. Complete these 3 quick steps to ensure 100% gateway uptime:",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    lineHeight = 17.sp
                )

                HorizontalDivider(color = colors.border)

                // Step 1: Autostart
                OemStepCard(
                    stepNumber = "1",
                    title = "Enable Autostart / Auto-Launch",
                    description = guidance.autostartSteps.joinToString("\n"),
                    actionLabel = "Open Autostart Settings",
                    actionIcon = Icons.Default.Settings,
                    onAction = { OemWorkaroundHelper.openAutostartSettings(context) }
                )

                // Step 2: Disable Battery Saver
                OemStepCard(
                    stepNumber = "2",
                    title = "Disable Battery Saver ('No Restrictions')",
                    description = guidance.batterySaverSteps.joinToString("\n"),
                    actionLabel = "Disable Battery Saver",
                    actionIcon = Icons.Default.BatteryAlert,
                    onAction = { OemWorkaroundHelper.openBatterySaverSettings(context) }
                )

                // Step 3: Lock App in Recents
                OemStepCard(
                    stepNumber = "3",
                    title = "Lock App in Recent Apps Tray",
                    description = guidance.lockRecentsSteps.joinToString("\n"),
                    tipBadge = "Manual Action in Recents",
                    actionIcon = Icons.Default.Lock
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Action CTA
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextWhite,
                        contentColor = CanvasBlack
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = CanvasBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("I've Configured These Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun OemStepCard(
    stepNumber: String,
    title: String,
    description: String,
    actionLabel: String? = null,
    tipBadge: String? = null,
    actionIcon: ImageVector,
    onAction: (() -> Unit)? = null
) {
    val colors = PipraTheme.colors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.container,
                    border = BorderStroke(1.dp, colors.borderInteractive)
                ) {
                    Text(
                        text = "STEP $stepNumber",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            Text(
                text = description,
                fontSize = 11.sp,
                color = colors.textSecondary,
                lineHeight = 16.sp
            )

            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.borderInteractive),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.container,
                        contentColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = colors.textMuted
                    )
                }
            } else if (tipBadge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.container,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = tipBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentEmerald
                        )
                    }
                }
            }
        }
    }
}
