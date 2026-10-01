package co.pilas.precio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B5E20)
private val Yellow = Color(0xFFFFC107)

private val colors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0B2E0E),
    secondary = Color(0xFF8A6D00),
    secondaryContainer = Yellow,
    onSecondaryContainer = Color(0xFF3A2D00),
)

@Composable
fun PilasTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
