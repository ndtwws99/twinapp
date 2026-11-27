package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TwinLightColorScheme = lightColorScheme(
    primary = TwinPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = TwinPrimarySoftLight,
    onPrimaryContainer = Color(0xFF004B82),
    secondary = Color(0xFFD62976),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF831843),
    tertiary = Color(0xFF962FBF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF581C87),
    background = TwinBgLight,
    onBackground = TwinTextLight,
    surface = TwinSurfaceLight,
    onSurface = TwinTextLight,
    surfaceVariant = TwinSurface2Light,
    onSurfaceVariant = TwinText3Light,
    surfaceContainerLowest = TwinSurfaceLight,
    surfaceContainerLow = TwinBgLight,
    surfaceContainer = TwinSurface2Light,
    surfaceContainerHigh = TwinSurfaceHoverLight,
    outline = TwinBorderLight,
    outlineVariant = TwinBorderSubtleLight,
    error = TwinDangerLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF9F1239)
)

private val TwinDarkColorScheme = darkColorScheme(
    primary = TwinPrimaryDark,
    onPrimary = Color(0xFF041C32),
    primaryContainer = TwinPrimarySoftDark,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFFFB7185),
    onSecondary = Color(0xFF3F0615),
    secondaryContainer = Color(0xFF3B1525),
    onSecondaryContainer = Color(0xFFFFD5E0),
    tertiary = Color(0xFFC084FC),
    onTertiary = Color(0xFF280B42),
    tertiaryContainer = Color(0xFF2B1B40),
    onTertiaryContainer = Color(0xFFE9D5FF),
    background = TwinBgDark,
    onBackground = TwinTextDark,
    surface = TwinSurfaceDark,
    onSurface = TwinTextDark,
    surfaceVariant = TwinSurface2Dark,
    onSurfaceVariant = TwinText3Dark,
    surfaceContainerLowest = TwinBgDark,
    surfaceContainerLow = TwinSurfaceDark,
    surfaceContainer = TwinSurfaceElevatedDark,
    surfaceContainerHigh = TwinSurface2Dark,
    outline = TwinBorderDark,
    outlineVariant = TwinBorderSubtleDark,
    error = TwinDangerDark,
    onError = Color(0xFF3B080C),
    errorContainer = Color(0xFF3B1418),
    onErrorContainer = Color(0xFFFFD6D9)
)

val TwinShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun TwinTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TwinDarkColorScheme else TwinLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = TwinShapes,
        content = content
    )
}
