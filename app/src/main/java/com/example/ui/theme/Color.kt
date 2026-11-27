package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Twin Design System — Light Mode (from assets/css/twin.css)
val TwinPrimaryLight = Color(0xFF0095F6)
val TwinPrimaryHoverLight = Color(0xFF1877F2)
val TwinPrimarySoftLight = Color(0xFFE0F2FE)
val TwinBgLight = Color(0xFFF6F7F9)
val TwinSurfaceLight = Color(0xFFFFFFFF)
val TwinSurface2Light = Color(0xFFEEF0F4)
val TwinSurfaceHoverLight = Color(0xFFE3E7EF)
val TwinBorderLight = Color(0xFFE2E5EC)
val TwinBorderSubtleLight = Color(0xFFEEF0F4)
val TwinTextLight = Color(0xFF0F1419)
val TwinText2Light = Color(0xFF272C33)
val TwinText3Light = Color(0xFF687385)
val TwinDangerLight = Color(0xFFED4956)
val TwinSuccessLight = Color(0xFF1CD14F)
val TwinWarningLight = Color(0xFFF59E0B)

// Twin Design System — Dark Mode (from assets/css/twin.css)
val TwinPrimaryDark = Color(0xFF38A5FF)
val TwinPrimaryHoverDark = Color(0xFF60B8FF)
val TwinPrimarySoftDark = Color(0xFF132A42)
val TwinBgDark = Color(0xFF090C12)
val TwinSurfaceDark = Color(0xFF151A24)
val TwinSurface2Dark = Color(0xFF232B3D)
val TwinSurfaceElevatedDark = Color(0xFF1C2333)
val TwinBorderDark = Color(0xFF2E384D)
val TwinBorderSubtleDark = Color(0xFF232B3B)
val TwinTextDark = Color(0xFFF8FAFC)
val TwinText2Dark = Color(0xFFDCE4F2)
val TwinText3Dark = Color(0xFF9FB0C9)
val TwinDangerDark = Color(0xFFFF5C6A)
val TwinSuccessDark = Color(0xFF34D399)
val TwinWarningDark = Color(0xFFFBBF24)

// Twin Signature Accents & Story Gradient Ring
val TwinHeartRed = Color(0xFFFF3040)
val TwinStoryGradientColors = listOf(
    Color(0xFFFEDA75),
    Color(0xFFFA7E1E),
    Color(0xFFD62976),
    Color(0xFF962FBF),
    Color(0xFF4F5BD5)
)

fun twinStoryRingBrush(): Brush = Brush.linearGradient(TwinStoryGradientColors)

fun storyBackgroundBrush(bgId: String): Brush = when (bgId) {
    "ig" -> Brush.linearGradient(listOf(Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF)))
    "ocean" -> Brush.linearGradient(listOf(Color(0xFF0095F6), Color(0xFF2563EB), Color(0xFF4F46E5)))
    "emerald" -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF0F766E)))
    "sunset" -> Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFEC4899)))
    "midnight" -> Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)))
    else -> Brush.linearGradient(listOf(Color(0xFF0095F6), Color(0xFF962FBF)))
}
