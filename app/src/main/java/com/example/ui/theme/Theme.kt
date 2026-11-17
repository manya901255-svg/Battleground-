package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VortexDarkColorScheme = darkColorScheme(
    primary = CyberOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0710),
    onPrimaryContainer = CyberOrangeLight,
    secondary = ElectricBlueSafe,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF092B52),
    onSecondaryContainer = ElectricBlueZone,
    tertiary = HazardYellow,
    onTertiary = Color.Black,
    background = TacticalDarkBg,
    onBackground = TextPrimary,
    surface = TacticalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder,
    error = RedZoneBombardment,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek tactical dark esports theme
    dynamicColor: Boolean = false, // Keep consistent esports aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VortexDarkColorScheme,
        typography = Typography,
        content = content
    )
}
