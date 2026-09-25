package com.plusemon.bizlipay.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// BizliPay Modern Indigo / Violet Palette Tokens
// =========================================================================

// Primary & Accents
val Indigo600 = Color(0xFF4F46E5)       // Primary brand deep
val Indigo500 = Color(0xFF6366F1)       // Primary brand vibrant
val Cyan500 = Color(0xFF06B6D4)         // Accent cyan
val Cyan400 = Color(0xFF22D3EE)         // Lighter cyan for glow

// Canvas & Surfaces (Dark Slate Palette)
val Slate950 = Color(0xFF0A0F1D)       // Ultra-dark depth
val Slate900 = Color(0xFF0F172A)       // Dark Background canvas (#0F172A)
val Slate850 = Color(0xFF162032)       // Elevated container
val Slate800 = Color(0xFF1E293B)       // Surface / Cards (#1E293B)
val Slate700 = Color(0xFF334155)       // Borders & dividers
val Slate600 = Color(0xFF475569)       // Interactive borders
val Slate500 = Color(0xFF64748B)       // Muted secondary text
val Slate400 = Color(0xFF94A3B8)       // Neutral text
val Slate300 = Color(0xFFCBD5E1)       // Light neutral text
val Slate100 = Color(0xFFF1F5F9)       // Light surface
val Slate50 = Color(0xFFF8FAFC)        // Light canvas background

// Light Mode Tokens
val CanvasWhite = Slate50
val ContainerLight = Color.White
val SurfaceCardLight = Slate100
val BorderLight = Color(0xFFE2E8F0)
val BorderLightInteractive = Color(0xFFCBD5E1)

// Typography
val TextWhite = Color(0xFFF8FAFC)
val TextLight = Slate300
val TextMuted = Slate400
val TextSubtle = Slate500
val TextBlack = Color(0xFF0F172A)

// Status Indicators
val StatusEmerald = Color(0xFF10B981)   // Online / Synced (#10B981)
val StatusAmber = Color(0xFFF59E0B)     // Syncing / Warning (#F59E0B)
val StatusRose = Color(0xFFEF4444)      // Error / Disconnected (#EF4444)

// Ghost Badges (10% tint & 25% border)
val GhostEmeraldBg = Color(0x1A10B981)
val GhostEmeraldBorder = Color(0x4010B981)
val GhostAmberBg = Color(0x1AF59E0B)
val GhostAmberBorder = Color(0x40F59E0B)
val GhostRoseBg = Color(0x1AEF4444)
val GhostRoseBorder = Color(0x40EF4444)
val GhostCyanBg = Color(0x1A06B6D4)
val GhostCyanBorder = Color(0x4006B6D4)
val GhostIndigoBg = Color(0x266366F1)
val GhostIndigoBorder = Color(0x4D6366F1)

// MFS Provider References with Colored Sender Tags
val BkashPink = Color(0xFFE2136E)       // bKash: Pink
val NagadOrange = Color(0xFFF7941D)     // Nagad: Orange
val RocketPurple = Color(0xFF8C3494)    // Rocket: Purple
val UpayYellow = Color(0xFFEAB308)      // Upay: Yellow
val TapBlue = Color(0xFF0284C7)         // TAP: Blue
val IbblGreen = Color(0xFF059669)       // Islami Bank: Green

// Compatibility aliases
val AccentEmerald = StatusEmerald
val AccentAmber = StatusAmber
val AccentRose = StatusRose
val Amber300 = Color(0xFFFCD34D)
val CanvasBlack = Slate900
val ContainerDark = Slate800
val SurfaceCard = Slate800
val SurfaceCardMuted = Slate850
val BorderZinc800 = Slate700
val BorderZinc700 = Slate600
val TextZinc300 = Slate300
val TextZinc400 = Slate400
val TextZinc500 = Slate500
val EmeraldPrimary = StatusEmerald
val StatusSynced = StatusEmerald
val StatusPending = StatusAmber
val StatusFailed = StatusRose
val BrandIndigo = Indigo600
val BrandIndigoPressed = Indigo500
