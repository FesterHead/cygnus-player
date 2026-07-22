package com.festerhead.cygnusplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Custom Dark Color Scheme based on Monokai Pro (Filter Spectrum).
 * Prioritizes high-contrast Purple, Blue/Cyan, and Orange for accessibility.
 */
private val DarkColorScheme = darkColorScheme(
    primary = MonokaiPurple,
    secondary = MonokaiBlue,
    tertiary = MonokaiOrange,
    background = MonokaiBackground,
    surface = MonokaiBackground,
    onPrimary = MonokaiBackground,
    onSecondary = MonokaiBackground,
    onTertiary = MonokaiBackground,
    onBackground = MonokaiText,
    onSurface = MonokaiText,
    surfaceVariant = MonokaiBackground,
    onSurfaceVariant = MonokaiSecondaryText,
)

@Composable
fun CygnusPlayerTheme(
    content: @Composable () -> Unit,
) {
    // Cygnus Player is strictly Dark Mode (Monokai Pro).
    // System bar coloring is handled by enableEdgeToEdge() in MainActivity.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
