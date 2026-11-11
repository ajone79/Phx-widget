package com.phoenixeu168.widget.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Night = darkColorScheme(
    primary = Color(0xFFFFB347),
    onPrimary = Color(0xFF0B1030),
    background = Color(0xFF0B1026),
    surface = Color(0xFF0B1026),
    onBackground = Color(0xFFF1F3FF),
    onSurface = Color(0xFFF1F3FF),
    onSurfaceVariant = Color(0xFF8E96BE),
    secondaryContainer = Color(0xFF2A3470),
    onSecondaryContainer = Color(0xFFFFE2B3),
)

@Composable
fun PhxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Night, content = content)
}
