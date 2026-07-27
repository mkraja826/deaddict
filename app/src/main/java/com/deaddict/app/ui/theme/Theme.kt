package com.deaddict.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    secondary = Color(0xFF0F9D83),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF172033),
    error = Color(0xFFE76F51),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    secondary = Color(0xFF5EEAD4),
    background = Color(0xFF10131A),
    onBackground = Color(0xFFF8FAFC),
    error = Color(0xFFF4A08D),
)

@Composable
fun DeAddictTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

