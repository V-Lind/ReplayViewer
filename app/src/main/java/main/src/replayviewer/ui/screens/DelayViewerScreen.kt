package main.src.replayviewer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import main.src.replayviewer.model.DelayViewerViewModel


@Composable
fun DelayViewerScreen(
    viewModel: DelayViewerViewModel,
    navController: NavController
) {
    val currentFrame by viewModel.delayedFrame.collectAsState()
    val bufferState by viewModel.bufferState.collectAsState()


    Box(modifier = Modifier.fillMaxSize()) {
        currentFrame?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } ?: Text(
            text = "Buffering frames: ${bufferState.first}/${bufferState.second}",
            modifier = Modifier.align(Alignment.Center)
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Button(
                onClick = { navController.navigate("mediaPlayer") },
                enabled = currentFrame != null,
                content = { Text("Save to mediaplayer") }

            )
        }
    }
}