package main.src.replayviewer.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import main.src.replayviewer.composables.MediaPlayerButton
import main.src.replayviewer.composables.SpeedSelector
import main.src.replayviewer.model.DelayViewerViewModel

@Composable
fun MediaPlayerScreen(viewModel: DelayViewerViewModel) {

    val currentFrame by viewModel.currentMediaPlayerFrame.collectAsState()
    val mediaPlayer by remember(Unit) { mutableStateOf(viewModel.getMediaPlayer()) }
    val context = LocalContext.current
    var selectedSpeed by remember { mutableDoubleStateOf(1.0) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    var isPlaying by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isVideoSaveButtonEnabled by remember { mutableStateOf(true) }


    DisposableEffect(Unit) {
        onDispose {
            // Pause media player
            mediaPlayer.pause()
            isPlaying = false
        }
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    mediaPlayer.pause()
                    isPlaying = false
                }
            }
        }
    }

    LaunchedEffect(currentFrame) {
        sliderPosition = mediaPlayer.getSliderPosition()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        currentFrame?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            val scaleFactor = newScale / scale
                            offsetX = (offsetX + pan.x) * scaleFactor
                            offsetY = (offsetY + pan.y) * scaleFactor
                            scale = newScale
                        }
                    },
                contentScale = ContentScale.Fit
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            Spacer(modifier = Modifier.padding(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                content = {
                    Button(
                        onClick = {
                            isVideoSaveButtonEnabled = false
                            mediaPlayer.createVideo()
                            Toast.makeText(
                                context,
                                "Saving video to device gallery",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = isVideoSaveButtonEnabled,
                        content = { Text(if (isVideoSaveButtonEnabled) "Save video" else "Video saved") }
                    )
                    Button(
                        onClick = {
                            mediaPlayer.createImage()
                            Toast.makeText(
                                context,
                                "Saved current image to device gallery",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        content = { Text("Save current image") }
                    )
                }
            )
        }



        SpeedSelector(
            selectedSpeed = selectedSpeed,
            onSpeedSelected = { speed ->
                selectedSpeed = speed
                mediaPlayer.setSpeed(speed)
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp, bottom = 32.dp)
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = { newPosition ->
                    sliderPosition = newPosition
                    mediaPlayer.goToFrame(newPosition.toInt())
                    Log.d("MediaPlayerScreen", "Slider position: $newPosition")
                },
                interactionSource = interactionSource,
                valueRange = 0f..(mediaPlayer.getBufferFrameCount() - 1).toFloat(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.padding(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MediaPlayerButton(
                    icon = if (isPlaying) Icons.Default.Clear else Icons.Default.PlayArrow,
                    onClick = {
                        if (isPlaying) {
                            mediaPlayer.pause()
                        } else {
                            mediaPlayer.play()
                        }
                        isPlaying = !isPlaying
                    }
                )
                MediaPlayerButton(icon = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                    onClick = {
                        mediaPlayer.previousFrame()
                        isPlaying = false
                    }
                )
                MediaPlayerButton(icon = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    onClick = {
                        mediaPlayer.nextFrame()
                        isPlaying = false
                    }
                )
            }
            Spacer(modifier = Modifier.padding(8.dp))

        }
    }
}

