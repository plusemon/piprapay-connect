package com.plusemon.bizlipay.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plusemon.bizlipay.ui.theme.Cyan500
import com.plusemon.bizlipay.ui.theme.Indigo500
import com.plusemon.bizlipay.ui.theme.Indigo600
import com.plusemon.bizlipay.ui.theme.BizliTheme
import com.plusemon.bizlipay.ui.theme.StatusAmber
import com.plusemon.bizlipay.ui.theme.StatusEmerald
import com.plusemon.bizlipay.ui.theme.StatusRose
import com.plusemon.bizlipay.ui.viewmodel.ServerSyncHealth
import com.plusemon.bizlipay.ui.viewmodel.ServerSyncStatus

/**
 * Modern BizliPay Connect Icon:
 * Sleek Indigo/Cyan lightning bolt embedded in a rounded container with gateway telemetry nodes.
 */
@Composable
fun BizliPayIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shapeCornerRadius: Dp = (size.value * 0.24f).dp,
    showBorder: Boolean = true
) {
    val colors = BizliTheme.colors
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(shapeCornerRadius),
        color = colors.surfaceCard,
        border = if (showBorder) BorderStroke(1.dp, colors.border) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.16f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height

                // Draw modern electric lightning bolt ("Bizli")
                val boltPath = Path().apply {
                    moveTo(w * 0.54f, 0f)
                    lineTo(w * 0.22f, h * 0.56f)
                    lineTo(w * 0.50f, h * 0.56f)
                    lineTo(w * 0.42f, h)
                    lineTo(w * 0.78f, h * 0.42f)
                    lineTo(w * 0.52f, h * 0.42f)
                    close()
                }

                drawPath(
                    path = boltPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Cyan500, Indigo500),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                // Small telemetry pulse dot in corner
                drawCircle(
                    color = StatusEmerald,
                    radius = w * 0.08f,
                    center = Offset(w * 0.82f, h * 0.18f)
                )
            }
        }
    }
}

@Composable
fun PipraPayIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shapeCornerRadius: Dp = (size.value * 0.24f).dp,
    showBorder: Boolean = true
) {
    BizliPayIcon(modifier, size, shapeCornerRadius, showBorder)
}

@Composable
fun BizliPayLogoLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 36.dp,
    titleFontSize: Int = 20,
    subtitle: String? = "Automated MFS Gateway Node",
    subtitleFontSize: Int = 11,
    statusHealth: ServerSyncHealth? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BizliPayIcon(size = iconSize)

        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "BizliPay",
                    fontWeight = FontWeight.Bold,
                    fontSize = titleFontSize.sp,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Connect",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = titleFontSize.sp,
                    letterSpacing = (-0.5).sp,
                    color = Indigo500
                )

                if (statusHealth != null) {
                    val dotColor = when (statusHealth.status) {
                        ServerSyncStatus.HEALTHY -> StatusEmerald
                        ServerSyncStatus.WARNING -> StatusAmber
                        ServerSyncStatus.ERROR -> StatusRose
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .testTag("header_title_status_dot")
                    )
                }
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = subtitleFontSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PipraPayLogoLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 36.dp,
    titleFontSize: Int = 20,
    subtitle: String? = "Automated MFS Gateway Node",
    subtitleFontSize: Int = 11,
    statusHealth: ServerSyncHealth? = null
) {
    BizliPayLogoLockup(modifier, iconSize, titleFontSize, subtitle, subtitleFontSize, statusHealth)
}
