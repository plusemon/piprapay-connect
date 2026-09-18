package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Developer-First Dark Mode Tokens (Linear / Vercel / Raycast Aesthetic)
// =========================================================================

// Canvas & Surfaces
val CanvasBlack = Color(0xFF09090B)       // Deep pitch black (#09090b)
val ContainerDark = Color(0xFF121215)     // Cards & primary containers (#121215)
val SurfaceCard = Color(0xFF18181B)       // Elevated surfaces & wallet cards (zinc-900: #18181b)
val SurfaceCardMuted = Color(0xFF151518)  // Secondary surface

// Crisp 1px Borders
val BorderZinc800 = Color(0xFF27272A)     // 1px crisp border (zinc-800: #27272a)
val BorderZinc700 = Color(0xFF3F3F46)     // Focused / interactive border (zinc-700: #3f3f46)

// Typography
val TextWhite = Color(0xFFFAFAFA)         // High-contrast pure white (#fafafa)
val TextZinc300 = Color(0xFFD4D4D8)       // Neutral light gray (#d4d4d8)
val TextZinc400 = Color(0xFFA1A1AA)       // Cool gray (#a1a1aa)
val TextZinc500 = Color(0xFF71717A)       // Muted cool zinc (#71717a)

// Restrained Micro-Accents & Status (Small pulse dots or ghost badges 10% tint)
val AccentEmerald = Color(0xFF10B981)     // Online / Active: Emerald (#10b981)
val AccentAmber = Color(0xFFF59E0B)       // Warning: Amber (#f59e0b)
val Amber300 = Color(0xFFFCD34D)          // Amber text ribbon (#fcd34d)
val AccentRose = Color(0xFFF43F5E)        // Error / Failed: Rose/Red (#f43f5e)

// Ghost Badges (10% tint & 20% border)
val GhostEmeraldBg = Color(0x1A10B981)
val GhostEmeraldBorder = Color(0x3310B981)
val GhostAmberBg = Color(0x1AF59E0B)
val GhostAmberBorder = Color(0x33F59E0B)
val GhostRoseBg = Color(0x1AF43F5E)
val GhostRoseBorder = Color(0x33F43F5E)

// Backwards compatibility mappings for existing screens & themes
val EmeraldPrimary = AccentEmerald
val EmeraldLight = AccentEmerald
val EmeraldDark = Color(0xFF059669)
val EmeraldContainerDark = ContainerDark
val EmeraldContainerLight = BorderZinc800

val NavyDarkBg = CanvasBlack
val NavyDarkSurface = ContainerDark
val NavyDarkSurfaceVariant = SurfaceCard

val LightCanvasBg = CanvasBlack
val LightSurface = ContainerDark
val LightSurfaceVariant = SurfaceCard

// MFS Provider References
val BkashPink = Color(0xFFE2136E)
val NagadOrange = Color(0xFFF7941D)
val RocketPurple = Color(0xFF8C3494)
val UpayNavy = Color(0xFF005696)

// Sync Status Colors
val StatusSynced = AccentEmerald
val StatusPending = AccentAmber
val StatusFailed = AccentRose

// Slate Tokens mapped to developer palette
val SlateDark = CanvasBlack
val SlateCard = ContainerDark
val SlateStroke = BorderZinc800
val SlateTextMuted = TextZinc500
val SlateSurfaceLight = SurfaceCard

// Standard M3 neutrals
val NeutralGrayText = TextZinc500
val NeutralDarkText = TextWhite
val NeutralLightText = TextWhite

// Authentication brand accents
val BrandIndigo = Color(0xFF5451F6)
val BrandIndigoPressed = Color(0xFF433FE0)
val BrandIndigoLight = SurfaceCard
val BrandIndigoRing = BorderZinc800
val InputBorderLight = BorderZinc800
val InputBackgroundLight = ContainerDark

