package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.AlertManager

/**
 * Obsidian-styled developer confirmation alert dialog for destructive actions
 * (clear, delete, disconnect) preventing accidental data loss.
 *
 * Adheres to industrial dark mode tokens:
 * - Dialog Container: #121215 with 1px border #27272A (border-zinc-800) and rounded-2xl (16dp)
 * - Backdrop Scrim: Darkened blur / bg-black/80
 * - Typography: Title in bold pure white (#FAFAFA), description in muted zinc (#71717A)
 * - Cancel Button: Neutral ghost button (border #3F3F46, text #D4D4D8, rounded-xl)
 * - Confirm/Destructive Button: High-visibility solid red (bg-rose-600 #E11D48, text white, rounded-xl)
 */
@Composable
fun PipraConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.Warning,
    testTag: String = "confirmation_dialog"
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Full screen backdrop scrim: Darkened blur / bg-black/80
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Dialog container surface
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept click so inner dialog taps don't dismiss
                    )
                    .testTag(testTag),
                shape = RoundedCornerShape(16.dp), // rounded-2xl
                color = Color(0xFF121215), // Dialog Container: #121215
                border = BorderStroke(1.dp, Color(0xFF27272A)) // 1px border border-zinc-800
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Visual icon header badge if provided
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE11D48).copy(alpha = 0.12f))
                                .border(
                                    BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.25f)),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Dialog Header & Description
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFAFAFA), // Title in bold pure white (#FAFAFA)
                            modifier = Modifier.testTag("${testTag}_title")
                        )

                        Text(
                            text = message,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF71717A), // description in muted zinc (#71717A)
                            modifier = Modifier.testTag("${testTag}_message")
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Dialog Action Buttons (Cancel + Confirm)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel Button: Neutral ghost button (border border-zinc-700 text-zinc-300 rounded-xl)
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("${testTag}_cancel_button"),
                            shape = RoundedCornerShape(12.dp), // rounded-xl
                            border = BorderStroke(1.dp, Color(0xFF3F3F46)), // border-zinc-700
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFFD4D4D8)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = cancelLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD4D4D8) // text-zinc-300
                            )
                        }

                        // Confirm/Destructive Button: High-visibility solid red (bg-rose-600 text-white rounded-xl font-medium)
                        Button(
                            onClick = {
                                AlertManager.triggerDestructiveHaptic(context)
                                onConfirm()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("${testTag}_confirm_button"),
                            shape = RoundedCornerShape(12.dp), // rounded-xl
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE11D48), // bg-rose-600
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = confirmLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
