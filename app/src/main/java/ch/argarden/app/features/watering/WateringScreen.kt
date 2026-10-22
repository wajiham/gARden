package ch.argarden.app.features.watering

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ch.argarden.app.ui.GardenPage

@Composable fun WateringScreen(back: () -> Unit) {
    GardenPage("Watering", back) { Text("Watering System — Coming in Part 3") }
}
