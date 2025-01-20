package main.src.replayviewer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import main.src.replayviewer.model.DelayViewerViewModel
import main.src.replayviewer.util.getAspectRatio

@Composable
fun MainMenuScreen(
    navController: NavController,
    viewModel: DelayViewerViewModel
) {
    val preferences by viewModel.preferences.collectAsState()
    val activePreference = preferences.find { it.isActive }
    val isCameraRunning by viewModel.isCameraRunning.collectAsState()


    Column(modifier = Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.padding(24.dp))
        activePreference?.let {
            Text("Active configuration: ${it.name}")
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 80.dp).fillMaxWidth()
            ) {
                item { Text("Description: \n${it.description}") }
            }

            Spacer(modifier = Modifier.padding(8.dp))
            Text("Resolution: ${it.resolution} (${getAspectRatio(it.resolution)})")
            Text("Frame rate: ${it.frameRate} fps")
            Text("Zoom level: ${(it.zoomLevel * 100).toInt()}%")
            Text("Delay length: ${it.delayLength} seconds")
            Text("Media player clip length: ${it.mediaPlayerClipLength} seconds")
            Spacer(modifier = Modifier.padding(16.dp))
        }

        Text("View and edit recording configurations")
        Button(
            onClick = { navController.navigate(Screen.Settings.route) },
            content = { Text("Saved recording configurations") }
        )

        if (!isCameraRunning) {
            Spacer(modifier = Modifier.padding(24.dp))
            Text(
                text = "Camera has been stopped.",
                color = MaterialTheme.colorScheme.error
                )
            Spacer(modifier = Modifier.padding(8.dp))
            Text("If this problem persists, make a new configuration with lower resolution or frame rates.")
            Button(
                onClick = { viewModel.resetMediaRepository() },
                content = { Text("Restart camera") }
            )
        } else {
            Spacer(modifier = Modifier.padding(24.dp))


            Text("View realtime video and create new recording configurations")
            Button(
                onClick = { navController.navigate(Screen.RealtimeViewer.route) },
                content = { Text("Open Realtime Viewer") }
            )
            Spacer(modifier = Modifier.padding(24.dp))


            Text("View delayed video stream and record clips")
            Button(
                onClick = { navController.navigate(Screen.DelayViewer.route) },
                content = { Text("Open Delay Viewer") }
            )

        }
    }
}