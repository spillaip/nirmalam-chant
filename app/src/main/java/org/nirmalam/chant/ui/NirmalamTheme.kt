package org.nirmalam.chant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NirmalamColors = darkColorScheme(
    primary = Color(0xFFF0BE71), secondary = Color(0xFFD6E9E0),
    background = Color(0xFF10201E), surface = Color(0xFF19302C), onBackground = Color(0xFFF7F5EE)
)

@Composable fun NirmalamTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = NirmalamColors, content = content)
