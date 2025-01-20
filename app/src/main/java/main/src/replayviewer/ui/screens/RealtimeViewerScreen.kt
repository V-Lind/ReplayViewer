package main.src.replayviewer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import main.src.replayviewer.model.RecordingConfiguration
import main.src.replayviewer.model.CameraProperties
import main.src.replayviewer.model.DelayViewerViewModel
import main.src.replayviewer.composables.OptionSelector
import main.src.replayviewer.util.getAspectRatio
import main.src.replayviewer.util.resolutionStringToSize

enum class SetupStage {
    INITIAL,
    NAME_PREFERENCE,
    SELECT_CAMERA,
    SELECT_ZOOM_LEVEL,
    SELECT_FRAME_RATE,
    SELECT_RESOLUTION,
    SELECT_STREAM_LENGTHS,
    CONFIRM_PREFERENCE_CREATION,

}

@Composable
fun RealtimeViewerScreen(
    viewModel: DelayViewerViewModel,
    fromSettings: Boolean = false
) {
    val realtimeFrame by viewModel.realtimeFrame.collectAsState()
    val currentPreference by remember { mutableStateOf(viewModel.getActivePreference(needsNewDefaults = true)) }


    val isNextEnabled = remember { mutableStateOf(true) }

    var currentSetupStage by remember { mutableStateOf(SetupStage.INITIAL) }

    fun changeSetupStage(direction: Int) {
        currentSetupStage = SetupStage.entries[currentSetupStage.ordinal + direction]
    }

    DisposableEffect(Unit) {
        onDispose {
            currentSetupStage = SetupStage.INITIAL
        }
    }

    // Automatically trigger setup if coming via settings screen
    LaunchedEffect(fromSettings) {
        if (fromSettings) {
            changeSetupStage(1)
        }
    }

    // Reset camera when starting camera setup
    LaunchedEffect(currentSetupStage) {
        if (currentSetupStage == SetupStage.NAME_PREFERENCE) {
            viewModel.resetMediaRepository(currentPreference)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        realtimeFrame?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart),
                contentScale = ContentScale.Fit
            )
        }
    }

    if (currentSetupStage == SetupStage.INITIAL) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
            content = {
                Button(
                    onClick = {
                        changeSetupStage(1)
//                        viewModel.resetMediaRepository(currentPreference)
                    },
                    modifier = Modifier.padding(16.dp),
                    content = { Text("Create new Configuration") }
                )
            }
        )
    } else {
        GetSetupStageContent(
            currentSetupStage,
            viewModel,
            currentPreference,
            resetSetupStage = { currentSetupStage = SetupStage.INITIAL },
            isNextEnabled = isNextEnabled
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
            content = {
                Button(
                    onClick = { changeSetupStage(-1) },
                    content = { Text("Back") }
                )
                if (currentSetupStage != SetupStage.CONFIRM_PREFERENCE_CREATION) {
                    Button(
                        onClick = { changeSetupStage(1) },
                        enabled = isNextEnabled.value,
                        content = { Text("Next") }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetSetupStageContent(
    currentSetupStage: SetupStage,
    viewModel: DelayViewerViewModel,
    currentPreference: RecordingConfiguration,
    resetSetupStage: () -> Unit,
    isNextEnabled: MutableState<Boolean>
) {
    val context = LocalContext.current
    val cameraOptions = viewModel.getAvailableCameraNames()
    val selectedCameraOption = remember { mutableStateOf(cameraOptions.first()) }

    var cameraProperties: CameraProperties? =
        viewModel.getCameraProperties(selectedCameraOption.value)


    // Name stuff
    var nameState by remember { mutableStateOf(currentPreference.name) }
    var descriptionState by remember { mutableStateOf(currentPreference.description) }


    // Zoom level
    var zoomLevel by remember { mutableFloatStateOf(currentPreference.zoomLevel) }

    // Autofocus
    var isAutofocusEnabled by remember { mutableStateOf(true) }
    var focusLevel by remember { mutableFloatStateOf(currentPreference.focusLevel) }


    // Frame rate options
    val frameRateOptions = cameraProperties?.fpsOptions?.map { it.toString() } ?: emptyList()

    val frameRateState = remember { mutableStateOf(currentPreference.frameRate.toString()) }


    // Resolution stuff
    val resolutionOptions = cameraProperties?.resolutionOptions?.map { it } ?: emptyList()

    val resolutionState =
        remember { mutableStateOf(resolutionStringToSize(currentPreference.resolution)) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    var showResolutionWarningDialog by remember { mutableStateOf(false) }


    // Stream lengths
    var availableDiskSpace by remember { mutableLongStateOf(viewModel.getAvailableDiskSpace()) }
    var delayLengthState by remember { mutableStateOf(currentPreference.delayLength.toString()) }
    var memoryWarningState by remember { mutableStateOf(false) }


    var mediaPlayerClipLengthState by
    remember { mutableStateOf(currentPreference.mediaPlayerClipLength.toString()) }
    val estimatedDiskSpaceRequired = remember { mutableLongStateOf(0) }

    var delayLengthErrorState by remember { mutableStateOf(false) }
    var mediaPlayerClipLengthErrorState by remember { mutableStateOf(false) }
    var mediaPlayerBiggerThanDelayErrorState by remember { mutableStateOf(false) }

    // Update estimated disk space required when delay length or media player clip length changes
    LaunchedEffect(currentPreference.delayLength, currentPreference.mediaPlayerClipLength) {
        availableDiskSpace = viewModel.getAvailableDiskSpace()
        estimatedDiskSpaceRequired.longValue =
            viewModel.getEstimatedDiskSpaceRequired(currentPreference)

        mediaPlayerBiggerThanDelayErrorState =
            currentPreference.mediaPlayerClipLength > currentPreference.delayLength

        memoryWarningState =
            availableDiskSpace < estimatedDiskSpaceRequired.longValue || estimatedDiskSpaceRequired.longValue < 0

        isNextEnabled.value = !delayLengthErrorState
                && !mediaPlayerClipLengthErrorState
                && !memoryWarningState
                && !mediaPlayerBiggerThanDelayErrorState
    }



    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Spacer(modifier = Modifier.padding(16.dp))
        when (currentSetupStage) {
            SetupStage.INITIAL -> {
                // Do nothing
            }

            SetupStage.NAME_PREFERENCE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter,
                    content = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = nameState,
                                onValueChange = {
                                    nameState = it
                                    currentPreference.name = it
                                },
                                label = { Text("Preference Name") }
                            )

                            OutlinedTextField(
                                value = descriptionState,
                                onValueChange = {
                                    descriptionState = it
                                    currentPreference.description = it
                                },
                                label = { Text("Preference Description") }
                            )
//                            Spacer(modifier = Modifier.padding(16.dp))
//                            Checkbox(
//                                checked = currentPreference.shouldReturnToRealtimeDisplay,
//                                onCheckedChange = {
//                                    currentPreference.shouldReturnToRealtimeDisplay = it
//
//                                }
//                            )
                        }
                    }
                )

            }

            SetupStage.SELECT_CAMERA -> {

                Text("Choose which camera to use: ")
                OptionSelector(
                    options = cameraOptions,
                    selectedOption = selectedCameraOption,
                    onSelect = { selection ->
                        cameraProperties = viewModel.getCameraProperties(selection)
                        currentPreference.cameraId = selection
                        currentPreference.cameraOrientation = cameraProperties!!.cameraOrientation
                        viewModel.resetMediaRepository(currentPreference)
                    }
                )

            }

            SetupStage.SELECT_ZOOM_LEVEL -> {

                Text("Select zoom level: ${(zoomLevel * 100).toInt()}%")
                Slider(
                    value = zoomLevel,
                    onValueChange = { newValue ->
                        zoomLevel = newValue
                        currentPreference.zoomLevel = newValue
                        viewModel.setCameraZoomLevel(newValue)
                    },
                    valueRange = 0.0f..1.0f,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.padding(horizontal = 16.dp)
//                ) {
//                    Checkbox(
//                        checked = isAutofocusEnabled,
//                        onCheckedChange = { isChecked ->
//                            isAutofocusEnabled = isChecked
//                            currentPreference.isAutofocusEnabled = isChecked
//                            viewModel.resetMediaRepository(currentPreference)
//                        }
//                    )
//                    Text("Autofocus")
//                }
//
//                if (!isAutofocusEnabled) {
//                    Text("Select focus level: ${(focusLevel * 100).toInt()}%")
//                    Slider(
//                        value = focusLevel,
//                        onValueChange = { newValue ->
//                            val focus = cameraProperties!!.maxFocalLength
//                            focusLevel = newValue
//                            currentPreference.focusLevel = newValue * focus
//                            viewModel.setCameraFocusLevel(newValue * focus)
//                        },
//                        valueRange = 0.0f..1.0f,
//                        modifier = Modifier.padding(horizontal = 16.dp)
//                    )
//                }

            }

            SetupStage.SELECT_FRAME_RATE -> {

                Text("Select frame rate: ")
                OptionSelector(
                    options = frameRateOptions,
                    selectedOption = frameRateState,
                    onSelect = { selection ->
                        frameRateState.value = selection
                        currentPreference.frameRate = selection.toInt()
                        viewModel.resetMediaRepository(currentPreference)
                    }
                )

            }

            SetupStage.SELECT_RESOLUTION -> {

                Text("Select resolution: ${resolutionState.value.width}x${resolutionState.value.height}")
                Button(
                    onClick = { showResolutionDialog = true },
                    content = { Text("Options") }
                )
                if (showResolutionDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                        content = {
                            BasicAlertDialog(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                onDismissRequest = { showResolutionDialog = false },
                                content = {
                                    OptionSelector(
                                        options = resolutionOptions,
                                        selectedOption = resolutionState,
                                        onSelect = { selection ->

                                            // Trigger warning dialogue if selected resolution is high
                                            if (selection.width * selection.height > (1920 * 1080)) {
                                                showResolutionWarningDialog = true
                                            }

                                            resolutionState.value = selection
                                            currentPreference.resolution =
                                                "${selection.width}x${selection.height}"
                                            viewModel.resetMediaRepository(currentPreference)
                                            showResolutionDialog = false
                                        },
                                        groupingOptions = { getAspectRatio(it) }
                                    )
                                }
                            )
                        }
                    )
                }
                if (showResolutionWarningDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showResolutionWarningDialog = false
                        },
                        title = { Text("Warning") },
                        text = {
                            Column {
                                Text("Selecting high resolutions (1920x1080 or higher) may cause the application to become unstable.")
                                Text("If this happens, select a lower resolution.")
                            }
                        },
                        confirmButton = {
                            Button({ showResolutionWarningDialog = false }) { Text("Ok") }
                        },
                    )
                }
            }


            SetupStage.SELECT_STREAM_LENGTHS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter,
                    content = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.padding(8.dp))
                            Column {
                                Text("Available disk space: $availableDiskSpace MB")
                                Text(
                                    "Estimated disk space required for \nthese buffer values: ${
                                        estimatedDiskSpaceRequired.longValue.takeIf { it >= 0 } ?: "Error: Overflow"
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
                                        currentPreference.delayLength = newValue
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
                                    text = "Error: \nMedia player clip length cannot be bigger than delay length",
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
                                        currentPreference.mediaPlayerClipLength = newValue
                                    }

                                    // Update error state on invalid input
                                    mediaPlayerClipLengthErrorState =
                                        newValue == null || newValue < 1

                                },
                                label = { Text("MediaPlayer Clip Length in seconds") },
                                isError = mediaPlayerClipLengthErrorState,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                )
                            )
                        }
                    }
                )
            }


            SetupStage.CONFIRM_PREFERENCE_CREATION -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter,
                    content = {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .fillMaxWidth(),

                        ) {
                            Spacer(modifier = Modifier.padding(24.dp))
                            Text("Confirm settings:")
                            Text("Name: ${currentPreference.name}")
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                            ) {
                                item {

                                    Text("Description: ${currentPreference.description}")
                                }
                            }
                            Text("Camera: ${currentPreference.cameraId}")
                            Text("Zoom level: ${(zoomLevel * 100).toInt()}%")
                            Text("Resolution: ${currentPreference.resolution}")
                            Text("Frame rate: ${currentPreference.frameRate} fps")
                            Text("Delay length: ${currentPreference.delayLength} seconds")
                            Text("Media player clip length: ${currentPreference.mediaPlayerClipLength} seconds")
                            Spacer(modifier = Modifier.padding(16.dp))

                            Button(
                                onClick = {
                                    viewModel.createPreference(currentPreference)
                                    resetSetupStage()
                                    Toast.makeText(
                                        context,
                                        "Preference created",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                content = { Text("Create Preference") }
                            )
                        }
                    }
                )
            }
        }
    }
}




