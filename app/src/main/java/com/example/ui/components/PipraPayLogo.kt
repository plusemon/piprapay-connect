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
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(shapeCornerRadius),
        color = Color(0xFF09090B),
        border = if (showBorder) BorderStroke(1.dp, BorderZinc800.copy(alpha = 0.8f)) else null
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

                // 1. White Stem of "P"
                val stemWidth = w * 0.22f
                val stemCorner = stemWidth / 2f
                drawRoundRect(
                    color = Color(0xFFFAFAFA),
                    topLeft = Offset(0f, 0f),
                    size = Size(stemWidth, h),
                    cornerRadius = CornerRadius(stemCorner, stemCorner)
                )

                // 2. White Loop of "P"
                val loopTop = 0f
                val loopHeight = h * 0.60f
                val loopThickness = stemWidth * 0.85f
                val loopRight = w * 0.82f

                val outerPath = Path().apply {
                    moveTo(stemWidth * 0.5f, loopTop)
                    lineTo(loopRight - loopHeight / 2f, loopTop)
                    // Outer arc on the right
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = loopRight - loopHeight,
                            top = loopTop,
                            right = loopRight,
                            bottom = loopTop + loopHeight
                        ),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                    lineTo(stemWidth * 0.5f, loopTop + loopHeight)
                    lineTo(stemWidth * 0.5f, loopTop + loopHeight - loopThickness)
                    lineTo(loopRight - loopHeight / 2f, loopTop + loopHeight - loopThickness)
                    // Inner arc on the right
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = loopRight - loopHeight + loopThickness,
                            top = loopTop + loopThickness,
                            right = loopRight - loopThickness,
                            bottom = loopTop + loopHeight - loopThickness
                        ),
                        startAngleDegrees = 90f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    lineTo(stemWidth * 0.5f, loopTop + loopThickness)
                    close()
                }
                drawPath(outerPath, color = Color(0xFFFAFAFA), style = Fill)

                // 3. Emerald Connect Node at loop apex/apex terminal
                val nodeRadius = w * 0.14f
                val nodeCenter = Offset(loopRight, loopTop + loopHeight / 2f)

                // Outer Emerald Halo
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = nodeRadius,
                    center = nodeCenter
                )

                // Inner Bright Node Highlight
                drawCircle(
                    color = Color(0xFF6EE7B7),
                    radius = nodeRadius * 0.48f,
                    center = nodeCenter
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
                    color = TextWhite
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
                    color = TextZinc400
                )
            }
        }
    }
}
