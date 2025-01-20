package main.src.replayviewer.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import main.src.replayviewer.model.RecordingConfiguration
import main.src.replayviewer.model.DelayViewerViewModel
import main.src.replayviewer.util.getAspectRatio


@Composable
fun SettingsScreen(
    viewModel: DelayViewerViewModel,
    navController: NavController
) {
    val preferences by viewModel.preferences.collectAsState()
    val expandedStates = remember { mutableStateListOf(*Array(preferences.size) { false }) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Recording configurations", fontSize = 24.sp, modifier = Modifier.padding(24.dp))
        LazyColumn {
            items(preferences.size) { index ->
                PreferenceItem(
                    recordingConfiguration = preferences[index],
                    isExpanded = expandedStates[index],
                    isActive = preferences[index].isActive,
                    onExpandChange = { expandedStates[index] = it },
                    viewModel = viewModel,
                    preferences = preferences,
                    context = context,
                    index = index
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
            content = {
                Button(
                    onClick = {
                        navController.navigate("${Screen.RealtimeViewer.route}/fromSettings") {
                            popUpTo(Screen.Settings.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(16.dp),
                    content = { Text("Create new Configuration") }
                )
            }
        )
    }
}


@Composable
fun PreferenceItem(
    recordingConfiguration: RecordingConfiguration,
    isExpanded: Boolean,
    isActive: Boolean,
    onExpandChange: (Boolean) -> Unit,
    viewModel: DelayViewerViewModel,
    preferences: List<RecordingConfiguration>,
    context: Context,
    index: Int
) {
    val cameraInUse = when (recordingConfiguration.cameraId) {
        "BACK" -> "Back"
        else -> "Front"
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandChange(!isExpanded) }
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = recordingConfiguration.name, modifier = Modifier.padding(start = 8.dp))
                Row {
                    Text(
                        text = if (isActive) "Active" else "Inactive",
                        color = if (isActive) Color.Green else Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Icon(
                        imageVector =
                        if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }
        }
        if (isExpanded) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Uses camera: $cameraInUse")
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("Description: ")
                    Text(recordingConfiguration.description)
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Text("Frame rate: ${recordingConfiguration.frameRate} fps")
                Text("Resolution: ${recordingConfiguration.resolution} (${getAspectRatio(recordingConfiguration.resolution)})")
                Text("Delay Length: ${recordingConfiguration.delayLength}")
                Text("MediaPlayer Clip Length: ${recordingConfiguration.mediaPlayerClipLength}")
                Text("Zoom Level: ${(recordingConfiguration.zoomLevel * 100).toInt()}%")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            viewModel.changeActivePreference(preferences[index].id)
                        },
                        content = { Text("Set Active") }
                    )

                    Button(
                        onClick = {
                            if (preferences[index].isActive) {
                                Toast.makeText(
                                    context,
                                    "Cannot delete active preference",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.deletePreference(preferences[index].id)
                            }
                        },
                        content = { Text("Delete") }
                    )
                }
            }
        }
    }
}






