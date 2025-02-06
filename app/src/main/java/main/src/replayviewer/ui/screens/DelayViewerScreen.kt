package main.src.replayviewer.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import main.src.replayviewer.model.DelayViewerViewModel
import main.src.replayviewer.model.FrameType


@Composable
fun DelayViewerScreen(
    viewModel: DelayViewerViewModel, navController: NavController
) {
    val delayedFrame by viewModel.delayedFrame.collectAsState()
    val realtimeFrame by viewModel.realtimeFrame.collectAsState()
    val bufferState by viewModel.bufferState.collectAsState()

    // Zoom level
    var zoomLevel by remember { mutableFloatStateOf(viewModel.getActivePreference().zoomLevel) }

    @Composable
    fun ImageContainer(modifier: Modifier) {
        if (viewModel.getWhichFrameBeingEmitted() == FrameType.DELAYED) {
            delayedFrame?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = modifier,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        } else {
            realtimeFrame?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = modifier,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        ImageContainer(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
        )


        // Vertical zoom slider
        if (viewModel.getWhichFrameBeingEmitted() == FrameType.REALTIME) {

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${(zoomLevel * 100).toInt()}%")
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "",
                )

                Slider(
                    value = zoomLevel,
                    onValueChange = { newValue ->
                        zoomLevel = newValue
                        viewModel.setCameraZoomLevel(
                            zoomLevel = newValue,
                            updatePreference = true
                        )
                    },
                    valueRange = 0.0f..1.0f,
                    modifier = Modifier
                        .height(300.dp)

                        // Flip slider and constraints from horizontal to vertical
                        .graphicsLayer {
                            rotationZ = 270f
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                Constraints(
                                    minWidth = constraints.minHeight,
                                    maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth,
                                    maxHeight = constraints.maxWidth
                                )
                            )
                            layout(placeable.height, placeable.width) {
                                placeable.place(-placeable.width, 0)
                            }
                        }
                )
            }
        }



        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.padding(8.dp))
            Text("Displaying ${if (viewModel.getWhichFrameBeingEmitted() == FrameType.DELAYED) "delayed" else "realtime"} video")
            if (bufferState.first < bufferState.second) {
                Text("Buffering video delay: ${bufferState.first}/${bufferState.second}")
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Space evenly
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Button(
                    onClick = {
                        // Switch which frames should be collected
                        with(viewModel) {
                            if (getWhichFrameBeingEmitted() == FrameType.DELAYED) {
                                setShouldEmitRealtimeFrames(true)
                                setShouldEmitDelayedFrames(false)
                            } else {
                                setShouldEmitDelayedFrames(true)
                                setShouldEmitRealtimeFrames(false)
                            }
                        }
                    },
                    shape = androidx.compose.material3.MaterialTheme.shapes.small,
                    modifier = Modifier.padding(4.dp),
                    content = {
                        Text("Switch to ${if (viewModel.getWhichFrameBeingEmitted() == FrameType.DELAYED) "realtime" else "delayed"}\n display")
                    }

                )
                if (viewModel.getWhichFrameBeingEmitted() == FrameType.DELAYED) {
                    Button(
                        onClick = { navController.navigate("mediaPlayer") },
                        enabled = delayedFrame != null,
                        shape = androidx.compose.material3.MaterialTheme.shapes.small,
                        modifier = Modifier.padding(4.dp),
                        content = {
                            Text("Replay last ${viewModel.getActivePreference().mediaPlayerClipLength} seconds")
                        })
                }
            }
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }
}