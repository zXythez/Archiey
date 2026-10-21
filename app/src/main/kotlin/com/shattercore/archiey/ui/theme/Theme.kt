package com.shattercore.archiey.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArchieyColors = darkColorScheme(
    primary = Color(0xFF86D99A),
    background = Color(0xFF101412),
    surface = Color(0xFF181D1A),
    onBackground = Color(0xFFF1F4F1),
    onSurface = Color(0xFFF1F4F1)
)

@Composable
fun ArchieyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArchieyColors,
        content = content
    )
}
