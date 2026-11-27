package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = PulseEmerald,
    onPrimary = Color(0xFF04210C),
    primaryContainer = PulseEmeraldContainerDark,
    onPrimaryContainer = Color(0xFFA6F4B8),
    secondary = PulseCyan,
    onSecondary = Color(0xFF051D38),
    secondaryContainer = PulseCyanContainerDark,
    onSecondaryContainer = Color(0xFFCBE5FF),
    tertiary = PulseViolet,
    onTertiary = Color(0xFF1F0D3A),
    tertiaryContainer = PulseVioletContainerDark,
    onTertiaryContainer = Color(0xFFE4D3FF),
    background = ObsidianCanvas,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = ObsidianTextSecondary,
    surfaceContainerLowest = ObsidianCanvas,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF21262D),
    error = PulseRose,
    onError = Color(0xFF3E0608),
    errorContainer = PulseRoseContainerDark,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val DaylightLightColorScheme = lightColorScheme(
    primary = PulseEmeraldDark,
    onPrimary = Color.White,
    primaryContainer = PulseEmeraldContainerLight,
    onPrimaryContainer = Color(0xFF0E4429),
    secondary = PulseCyanDark,
    onSecondary = Color.White,
    secondaryContainer = PulseCyanContainerLight,
    onSecondaryContainer = Color(0xFF053B7A),
    tertiary = PulseVioletDark,
    onTertiary = Color.White,
    tertiaryContainer = PulseVioletContainerLight,
    onTertiaryContainer = Color(0xFF3C1E70),
    background = DaylightCanvas,
    onBackground = DaylightTextPrimary,
    surface = DaylightSurface,
    onSurface = DaylightTextPrimary,
    surfaceVariant = DaylightSurfaceElevated,
    onSurfaceVariant = DaylightTextSecondary,
    surfaceContainerLowest = DaylightSurface,
    surfaceContainerLow = DaylightCanvas,
    surfaceContainer = DaylightSurfaceElevated,
    surfaceContainerHigh = DaylightSurfaceHighest,
    outline = DaylightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFCF222E),
    onError = Color.White,
    errorContainer = PulseRoseContainerLight,
    onErrorContainer = Color(0xFF82071E)
)

val GitPulseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun GitPulseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ObsidianDarkColorScheme else DaylightLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = GitPulseShapes,
        content = content
    )
}
