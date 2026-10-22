package ch.argarden.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GardenColors = lightColorScheme(
    primary = Color(0xFF276443), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EEDC), secondary = Color(0xFF526457),
    background = Color(0xFFF7F9F3), surface = Color(0xFFF7F9F3)
)
@Composable fun GardenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GardenColors, content = content)
}
