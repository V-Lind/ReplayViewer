package main.src.replayviewer.ui.screens

import android.media.Image
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.serialization.json.JsonNull.content
import main.src.replayviewer.model.DelayViewerViewModel
import main.src.replayviewer.model.FrameType


@Composable
fun DelayViewerScreen(
    viewModel: DelayViewerViewModel, navController: NavController
) {
    val delayedFrame by viewModel.delayedFrame.collectAsState()
    val realtimeFrame by viewModel.realtimeFrame.collectAsState()
    val bufferState by viewModel.bufferState.collectAsState()

    @Composable
    fun UpdateImage(modifier: Modifier) {
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
        UpdateImage(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopStart),
        )
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
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,


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
                Button(onClick = { navController.navigate("mediaPlayer") },
                    enabled = delayedFrame != null,
                    shape = androidx.compose.material3.MaterialTheme.shapes.small,
                    modifier = Modifier.padding(4.dp),
                    content = {
                        Text("Replay last ${viewModel.getActivePreference().mediaPlayerClipLength} seconds")
                    })
            }
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }
}