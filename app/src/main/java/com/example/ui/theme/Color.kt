package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Obsidian Developer Social — Dark Palette
val ObsidianCanvas = Color(0xFF0D1117)
val ObsidianSurface = Color(0xFF161B22)
val ObsidianSurfaceElevated = Color(0xFF1C232D)
val ObsidianSurfaceHighest = Color(0xFF232C38)
val ObsidianBorder = Color(0xFF30363D)
val ObsidianTextPrimary = Color(0xFFF0F6FC)
val ObsidianTextSecondary = Color(0xFF8B949E)

// Crisp Daylight — Light Palette
val DaylightCanvas = Color(0xFFF6F8FA)
val DaylightSurface = Color(0xFFFFFFFF)
val DaylightSurfaceElevated = Color(0xFFF0F3F6)
val DaylightSurfaceHighest = Color(0xFFE6EBF1)
val DaylightBorder = Color(0xFFD0D7DE)
val DaylightTextPrimary = Color(0xFF1F2328)
val DaylightTextSecondary = Color(0xFF59636E)

// Brand & Semantic Accents
val PulseEmerald = Color(0xFF3FB950)
val PulseEmeraldDark = Color(0xFF238636)
val PulseEmeraldContainerDark = Color(0xFF132D1B)
val PulseEmeraldContainerLight = Color(0xFFDDF4E4)

val PulseCyan = Color(0xFF58A6FF)
val PulseCyanDark = Color(0xFF0969DA)
val PulseCyanContainerDark = Color(0xFF11253D)
val PulseCyanContainerLight = Color(0xFFDDF4FF)

val PulseViolet = Color(0xFFA371F7)
val PulseVioletDark = Color(0xFF8250DF)
val PulseVioletContainerDark = Color(0xFF251A3A)
val PulseVioletContainerLight = Color(0xFFF3EBFF)

val PulseAmber = Color(0xFFD29922)
val PulseRose = Color(0xFFF85149)
val PulseRoseContainerDark = Color(0xFF3B1517)
val PulseRoseContainerLight = Color(0xFFFFEBE9)

fun languageColor(language: String?): Color = when (language?.lowercase()) {
    "kotlin" -> Color(0xFFA97BFF)
    "java" -> Color(0xFFB07219)
    "typescript" -> Color(0xFF3178C6)
    "javascript" -> Color(0xFFF1E05A)
    "python" -> Color(0xFF3572A5)
    "rust" -> Color(0xFFDEA584)
    "go" -> Color(0xFF00ADD8)
    "swift" -> Color(0xFFF05138)
    "dart" -> Color(0xFF00B4AB)
    "c++", "cpp" -> Color(0xFFF34B7D)
    "c#" -> Color(0xFF178600)
    "c" -> Color(0xFF555555)
    "vue" -> Color(0xFF41B883)
    "html" -> Color(0xFFE34C26)
    "css", "scss" -> Color(0xFF563D7C)
    "ruby" -> Color(0xFF701516)
    "php" -> Color(0xFF4F5D95)
    "shell" -> Color(0xFF89E051)
    else -> PulseCyan
}

fun parseGitHubHexColor(hex: String?, fallback: Color = PulseCyan): Color {
    if (hex.isNullOrBlank()) return fallback
    val cleaned = hex.trim().removePrefix("#")
    return try {
        val value = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
