package com.plusemon.bizlipay.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plusemon.bizlipay.data.prefs.SUPPORTED_MFS_SENDERS
import com.plusemon.bizlipay.ui.theme.AccentEmerald
import com.plusemon.bizlipay.ui.theme.GhostEmeraldBg
import com.plusemon.bizlipay.ui.theme.GhostEmeraldBorder
import com.plusemon.bizlipay.ui.theme.BizliTheme
import com.plusemon.bizlipay.ui.viewmodel.MainViewModel

@Composable
fun SendersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = BizliTheme.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isSyncingSenders by remember { mutableStateOf(false) }

    val gateways = remember(settings.gatewayGroups, settings.whitelistedSenders) {
        if (settings.gatewayGroups.isNotEmpty()) settings.gatewayGroups else SUPPORTED_MFS_SENDERS
    }

    val activeCount = remember(gateways, settings.whitelistedSenders) {
        gateways.count { viewModel.isSenderEnabled(it.id) }
    }

    val filteredGateways = remember(gateways, searchQuery, settings.whitelistedSenders) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            gateways
        } else {
            gateways.filter { config ->
                config.displayName.lowercase().contains(q) ||
                    config.id.lowercase().contains(q) ||
                    config.allAliases.any { it.lowercase().contains(q) }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasBg)
            .testTag("senders_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Overview & Panel Sync Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("senders_overview_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GhostEmeraldBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Telephony Whitelist",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Strict regex sender isolation",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Active sender count badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GhostEmeraldBg,
                            border = BorderStroke(1.dp, GhostEmeraldBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentEmerald)
                                )
                                Text(
                                    text = "$activeCount / ${gateways.size} Active",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEmerald
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colors.border, thickness = 1.dp)

                    // Sync Senders action button
                    OutlinedButton(
                        onClick = {
                            isSyncingSenders = true
                            viewModel.syncSendersFromPanel(
                                onSuccess = { result ->
                                    isSyncingSenders = false
                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    isSyncingSenders = false
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        enabled = !isSyncingSenders,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("sync_senders_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceCard,
                            contentColor = colors.textPrimary
                        ),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        if (isSyncingSenders) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AccentEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing Senders...", fontSize = 12.sp, color = colors.textMuted)
                        } else {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sync Senders from Panel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Search & Filter input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("senders_search_input"),
                placeholder = { Text("Filter senders by name or shortcode...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = colors.textPrimary),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.container,
                    unfocusedContainerColor = colors.container,
                    focusedBorderColor = AccentEmerald,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedPlaceholderColor = colors.textSubtle,
                    unfocusedPlaceholderColor = colors.textSubtle
                )
            )
        }

        // Section header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SUPPORTED MFS GATEWAYS (${filteredGateways.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = colors.textMuted
                )

                // Quick toggle buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.surfaceCard,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = "Enable All",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentEmerald,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable {
                                    gateways.forEach { viewModel.toggleSender(it.id, true) }
                                    Toast.makeText(context, "All senders enabled", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.surfaceCard,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = "Disable All",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable {
                                    gateways.forEach { viewModel.toggleSender(it.id, false) }
                                    Toast.makeText(context, "All senders disabled", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }
        }

        // Dynamic Whitelisted Senders Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sms_sender_routing_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.container),
                border = BorderStroke(1.dp, colors.border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (filteredGateways.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching senders found for \"$searchQuery\"",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        filteredGateways.forEachIndexed { index, config ->
                            val isEnabled = viewModel.isSenderEnabled(config.id)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Active status indicator dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isEnabled) AccentEmerald else colors.textMuted.copy(alpha = 0.35f))
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = config.displayName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isEnabled) colors.textPrimary else colors.textMuted
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = colors.surfaceCard,
                                                border = BorderStroke(1.dp, colors.border)
                                            ) {
                                                Text(
                                                    text = config.allAliases.joinToString(", "),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.textMuted,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (isEnabled) "Active: routes & forwards SMS packets" else "Disabled: discarded before regex parsing",
                                            fontSize = 11.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.toggleSender(config.id, enabled)
                                        Toast.makeText(
                                            context,
                                            "${config.displayName} ${if (enabled) "enabled" else "disabled"}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.testTag("sender_toggle_${config.id}"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AccentEmerald,
                                        uncheckedThumbColor = colors.textMuted,
                                        uncheckedTrackColor = colors.border
                                    )
                                )
                            }

                            if (index < filteredGateways.lastIndex) {
                                HorizontalDivider(color = colors.border, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
