package com.nirmalamgroup.nirmalamchant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NirmalamColors = darkColorScheme(
    primary = Color(0xFFF5C36D), onPrimary = Color(0xFF302008),
    secondary = Color(0xFFD6E9E0), onSecondary = Color(0xFF0E2521),
    background = Color(0xFF071716), onBackground = Color(0xFFF7F5EE),
    surface = Color(0xFF17312D), onSurface = Color(0xFFF5F3EA),
    surfaceVariant = Color(0xFF24413C), onSurfaceVariant = Color(0xFFD0E5DC),
    outline = Color(0xFF88A99E)
)

@Composable fun NirmalamTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = NirmalamColors, content = content)
