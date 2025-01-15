package main.src.replayviewer.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SpeedSelector(
    selectedSpeed: Double,
    onSpeedSelected: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val speeds = listOf(0.2, 0.5, 0.8, 1.0, 1.5, 2.0)
    Column(
        modifier = modifier
    ) {
        speeds.forEach { speed ->
            Text(
                text = "${speed}x",
                color = if (speed == selectedSpeed) Color.Red else Color.Unspecified,
                modifier = Modifier
                    .clickable { onSpeedSelected(speed) }
                    .padding(8.dp)
            )
        }
    }
}