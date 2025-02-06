package main.src.replayviewer.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import main.src.replayviewer.model.DelayViewerViewModel
import main.src.replayviewer.model.RecordingConfiguration
import main.src.replayviewer.util.getAspectRatio

@Composable
fun MainMenuScreen(
    navController: NavController,
    viewModel: DelayViewerViewModel
) {
    var currentPreference = remember { mutableStateOf(viewModel.getActivePreference()) }
    val isCameraRunning by viewModel.isCameraRunning.collectAsState()
    var showChangeLengthsDialog by remember { mutableStateOf(false) }


    Column(modifier = Modifier.padding(16.dp)) {
        Spacer(modifier = Modifier.padding(16.dp))
        currentPreference.value.let {
            Text("Active configuration: ${it.name}")
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 80.dp)
                    .fillMaxWidth()
            ) {
                item { Text("Description: \n${it.description}") }
            }

            Spacer(modifier = Modifier.padding(8.dp))
            Text("Resolution: ${it.resolution} (${getAspectRatio(it.resolution)})")
            Text("Frame rate: ${it.frameRate} fps")
            Text("Zoom level: ${(it.zoomLevel * 100).toInt()}%")
            Text("Playback delay length: ${it.delayLength} seconds")
            Text("Replay length: ${it.mediaPlayerClipLength} seconds")
            Button(
                onClick = { showChangeLengthsDialog = true },
                content = { Text("Change lengths") }
            )
            Spacer(modifier = Modifier.padding(12.dp))
        }

        Text("View and edit saved configurations")
        Button(
            onClick = { navController.navigate(Screen.Settings.route) },
            content = { Text("Configurations") }
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
            Spacer(modifier = Modifier.padding(12.dp))

            Text("View video streams")
            Button(
                onClick = { navController.navigate(Screen.DelayViewer.route) },
                content = { Text("Stream Viewer") }
            )

        }
        if (showChangeLengthsDialog) {
            ChangeLengthsDialog(
                viewModel = viewModel,
                onConfirmPress = { showChangeLengthsDialog = false },
                currentPreferenceReference = currentPreference,
                onDismissRequest = { showChangeLengthsDialog = false }
            )
        }
    }
    if (isCameraRunning) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row {
                    Checkbox(checked = currentPreference.value.automaticallyMakeVideoOnOpenMediaPlayer,
                        onCheckedChange = {
                            currentPreference.value = currentPreference.value.copy(
                                automaticallyMakeVideoOnOpenMediaPlayer = it
                            )
                            viewModel.updateActivePreference(currentPreference.value)
                        }
                    )
                    Text("Automatically save video when opening media player")
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Row {
                    Checkbox(checked = currentPreference.value.automaticallySwitchToRealTimeStream,
                        onCheckedChange = {
                            currentPreference.value = currentPreference.value.copy(
                                automaticallySwitchToRealTimeStream = it
                            )
                            viewModel.updateActivePreference(currentPreference.value)
                        }
                    )
                    Text("Automatically switch to real time display in stream viewer")
                }
                Spacer(modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun ChangeLengthsDialog(
    viewModel: DelayViewerViewModel,
    onConfirmPress: () -> Unit,
    currentPreferenceReference: MutableState<RecordingConfiguration>,
    onDismissRequest: () -> Unit
) {
    var isConfirmEnabled by remember { mutableStateOf(false) }
    var currentPreference by remember { mutableStateOf(currentPreferenceReference.value.copy()) }

    // Stream lengths
    val availableDiskSpace by remember { mutableLongStateOf(viewModel.getAvailableDiskSpace()) }
    var delayLengthState by remember { mutableStateOf(currentPreference.delayLength.toString()) }
    var memoryWarningState by remember { mutableStateOf(false) }


    var mediaPlayerClipLengthState by
    remember { mutableStateOf(currentPreference.mediaPlayerClipLength.toString()) }
    var estimatedDiskSpaceRequired by remember { mutableLongStateOf(0) }

    var delayLengthErrorState by remember { mutableStateOf(false) }
    var mediaPlayerClipLengthErrorState by remember { mutableStateOf(false) }
    var mediaPlayerBiggerThanDelayErrorState by remember { mutableStateOf(false) }

    // Update estimated disk space required when delay length or media player clip length changes
    LaunchedEffect(currentPreference, currentPreference.mediaPlayerClipLength) {
        estimatedDiskSpaceRequired =
            viewModel.getEstimatedDiskSpaceRequired(currentPreference)

        mediaPlayerBiggerThanDelayErrorState =
            currentPreference.mediaPlayerClipLength > currentPreference.delayLength

        memoryWarningState =
            availableDiskSpace < estimatedDiskSpaceRequired || estimatedDiskSpaceRequired < 0

        isConfirmEnabled = !delayLengthErrorState
                && !mediaPlayerClipLengthErrorState
                && !memoryWarningState
                && !mediaPlayerBiggerThanDelayErrorState
    }


    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.padding(8.dp))
                Column {
                    Text("Available disk space: $availableDiskSpace MB")
                    Text(
                        "Estimated disk space required for \nthese buffer values: ${
                            estimatedDiskSpaceRequired.takeIf { it >= 0 } ?: "Error: Overflow"
                        } MB"
                    )
                }
                Spacer(modifier = Modifier.padding(16.dp))
                if (memoryWarningState) {
                    Text(
                        text = "Error: \nNot enough disk space available for these settings",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = delayLengthState,
                    onValueChange = {
                        val newValue = it.toIntOrNull()
                        delayLengthState = it

                        // Update preference value
                        if (newValue != null && newValue >= 1) {
                            currentPreference = currentPreference.copy(delayLength = newValue)
                            Log.d(
                                "ChangeLengthsDialog",
                                "New delay length: $currentPreference.delayLength"
                            )
                        }

                        // Update error state on invalid input
                        delayLengthErrorState = newValue == null || newValue < 1
                    },
                    label = { Text("Delay Length in seconds") },
                    isError = delayLengthErrorState,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )

                if (mediaPlayerBiggerThanDelayErrorState) {
                    Text(
                        text = "Error: \nReplay length cannot be longer than delay length",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = mediaPlayerClipLengthState,
                    onValueChange = {
                        val newValue = it.toIntOrNull()
                        mediaPlayerClipLengthState = it

                        // Update preference value
                        if (newValue != null && newValue >= 1) {
                            currentPreference =
                                currentPreference.copy(mediaPlayerClipLength = newValue)
                        }

                        // Update error state on invalid input
                        mediaPlayerClipLengthErrorState =
                            newValue == null || newValue < 1

                    },
                    label = { Text("Replay Length in seconds") },
                    isError = mediaPlayerClipLengthErrorState,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
                Spacer(modifier = Modifier.padding(16.dp))
                Row {
                    Button(
                        onClick = onDismissRequest,
                        content = { Text("Cancel") }
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Button(
                        onClick = {
                            // Update/save new preference values
                            viewModel.setActivePreferenceLengths(
                                currentPreference.delayLength,
                                currentPreference.mediaPlayerClipLength
                            )

                            // Update values on main menu screen
                            currentPreferenceReference.value = currentPreference
                            // Reset repository to apply new settings //TODO: Make a non-reset solution??
                            viewModel.resetMediaRepository(currentPreference)
                            onConfirmPress()
                        },
                        enabled = isConfirmEnabled,
                        content = { Text("Confirm") }
                    )
                }
            }
        }
    }
}