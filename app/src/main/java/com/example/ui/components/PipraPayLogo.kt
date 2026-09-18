package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PipraTheme
import com.example.ui.theme.BorderZinc800
import com.example.ui.theme.CanvasBlack
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TextZinc400
import com.example.ui.theme.TextZinc500

/**
 * Flat minimalist PipraPay Connect icon:
 * Monochrome "P" loop combined with an emerald connect node on #09090B background.
 */
@Composable
fun PipraPayIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shapeCornerRadius: Dp = (size.value * 0.22f).dp,
    showBorder: Boolean = true
) {
    val colors = PipraTheme.colors
    val iconStemAndLoopColor = if (colors.isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(shapeCornerRadius),
        color = colors.container,
        border = if (showBorder) BorderStroke(1.dp, colors.border) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.15f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val centerY = h / 2f
                val strokeW = w * 0.08f
                val r = w * 0.15f // radius of the arc

                // Left Connection Port Bracket
                val leftBracketPath = Path().apply {
                    moveTo(w * 0.44f, centerY - r)
                    lineTo(w * 0.37f, centerY - r)
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = w * 0.37f - r,
                            top = centerY - r,
                            right = w * 0.37f + r,
                            bottom = centerY + r
                        ),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    lineTo(w * 0.44f, centerY + r)
                }
                drawPath(
                    path = leftBracketPath,
                    color = iconStemAndLoopColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeW,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // Right Connection Port Bracket
                val rightBracketPath = Path().apply {
                    moveTo(w * 0.56f, centerY - r)
                    lineTo(w * 0.63f, centerY - r)
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = w * 0.63f - r,
                            top = centerY - r,
                            right = w * 0.63f + r,
                            bottom = centerY + r
                        ),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                    lineTo(w * 0.56f, centerY + r)
                }
                drawPath(
                    path = rightBracketPath,
                    color = iconStemAndLoopColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeW,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // Active Connector Bridge (Linking both ports)
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(w * 0.32f, centerY),
                    end = Offset(w * 0.68f, centerY),
                    strokeWidth = strokeW,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // Central Glowing Connector Node
                val outerNodeRadius = w * 0.11f
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = outerNodeRadius,
                    center = Offset(w / 2f, centerY)
                )

                // Inner Core Connection Highlight
                drawCircle(
                    color = Color(0xFF6EE7B7),
                    radius = outerNodeRadius * 0.5f,
                    center = Offset(w / 2f, centerY)
                )
            }
        }
    }
}

/**
 * Compact horizontal logo lockup: [Icon] PipraPay Connect
 * with subtitle: "Automated MFS Gateway Node"
 */
@Composable
fun PipraPayLogoLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 36.dp,
    titleFontSize: Int = 20,
    subtitle: String? = "Automated MFS Gateway Node",
    subtitleFontSize: Int = 11
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PipraPayIcon(size = iconSize)

        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "PipraPay",
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
                    color = AccentEmerald
                )
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
