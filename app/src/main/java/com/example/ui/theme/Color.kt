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

// Light Mode Tokens
val CanvasWhite = Color(0xFFF8FAFC)       // Crisp light slate background (#f8fafc)
val ContainerLight = Color(0xFFFFFFFF)    // Pure white cards & containers (#ffffff)
val SurfaceCardLight = Color(0xFFF1F5F9)  // Elevated light surfaces (slate-100: #f1f5f9)
val SurfaceCardMutedLight = Color(0xFFE2E8F0) // Secondary light surface (#e2e8f0)

// Crisp 1px Borders
val BorderZinc800 = Color(0xFF27272A)     // 1px crisp border (zinc-800: #27272a)
val BorderZinc700 = Color(0xFF3F3F46)     // Focused / interactive border (zinc-700: #3f3f46)
val BorderLight = Color(0xFFE2E8F0)       // 1px crisp light border (#e2e8f0)
val BorderLightInteractive = Color(0xFFCBD5E1) // Interactive light border (#cbd5e1)

// Typography
val TextWhite = Color(0xFFFAFAFA)         // High-contrast pure white (#fafafa)
val TextZinc300 = Color(0xFFD4D4D8)       // Neutral light gray (#d4d4d8)
val TextZinc400 = Color(0xFFA1A1AA)       // Cool gray (#a1a1aa)
val TextZinc500 = Color(0xFF71717A)       // Muted cool zinc (#71717a)

val TextBlack = Color(0xFF09090B)         // High-contrast dark text (#09090b)
val TextSlate700 = Color(0xFF334155)      // Slate dark secondary text (#334155)
val TextSlate500 = Color(0xFF64748B)      // Slate muted text (#64748b)
val TextSlate400 = Color(0xFF94A3B8)      // Slate subtle text (#94a3b8)

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

val LightCanvasBg = CanvasWhite
val LightSurface = ContainerLight
val LightSurfaceVariant = SurfaceCardLight

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
val SlateSurfaceLight = SurfaceCardLight

// Standard M3 neutrals
val NeutralGrayText = TextZinc500
val NeutralDarkText = TextWhite
val NeutralLightText = TextBlack

// Authentication brand accents
val BrandIndigo = Color(0xFF5451F6)
val BrandIndigoPressed = Color(0xFF433FE0)
val BrandIndigoLight = SurfaceCardLight
val BrandIndigoRing = BorderLight
val InputBorderLight = BorderLight
val InputBackgroundLight = ContainerLight

