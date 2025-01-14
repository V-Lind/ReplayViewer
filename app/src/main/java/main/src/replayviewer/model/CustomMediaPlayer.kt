package main.src.replayviewer.model

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import main.src.replayviewer.util.toBitmap

class CustomMediaPlayer(
    private val frameProcessor: FrameProcessor,
    private val _currentMediaPlayerFrame: MutableStateFlow<ImageBitmap?>,
    private val context: Context,

    ) {

    private var isPlaying = false
    private var playbackSpeed = 1.0
    private var frameIndex = 0
    private var playerJob: Job? = null
    private var videoFrameRate = 30
    private var videoMaker: VideoMaker? = null
    private val bufferFrameCount = frameProcessor.getMediaPlayerFrameCount()
    private val mediaPlayerFrameList = frameProcessor.getMediaPlayerFiles()

    private var imgWidth = 0
    private var imgHeight = 0


    init { loadFrame(frameIndex) }


    fun play() {
        if (isPlaying) return
        Log.d("CustomMediaPlayer", "$bufferFrameCount")
        Log.d("CustomMediaPlayer", "$mediaPlayerFrameList")

        isPlaying = true

        // Start updating frames on playerJob
        playerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isPlaying) {
                Log.d("CustomMediaPlayer", "Playing frame $frameIndex")
                loadFrame(frameIndex)
                frameIndex = (frameIndex + 1) % bufferFrameCount
                delay((1000 / (videoFrameRate * playbackSpeed)).toLong())
            }
        }
    }

    fun pause() {
        isPlaying = false
        playerJob?.cancel()
    }

    fun nextFrame() {
        pause()
        frameIndex = (frameIndex + 1) % bufferFrameCount
        loadFrame(frameIndex)
    }

    fun previousFrame() {
        pause()
        if (frameIndex > 0) {
            frameIndex--
        } else {
            frameIndex = bufferFrameCount - 1
        }
        loadFrame(frameIndex)
    }

    fun setSpeed(speed: Double) {
        playbackSpeed = speed
    }

    private fun loadFrame(index: Int) {
        CoroutineScope(Dispatchers.IO).launch {

            // Select path of frame matching current index
            val filePath = mediaPlayerFrameList[index]
            Log.d("CustomMediaPlayer", "Loading frame at $filePath")

            // Load frame from disk
            val frameByteArray = frameProcessor.getMediaPlayerFrame(filePath)

            val bitmap = frameByteArray.toBitmap().asImageBitmap()

            imgWidth = bitmap.width
            imgHeight = bitmap.height

            // Emit frame to UI
            _currentMediaPlayerFrame.emit(bitmap)

        }
    }

    fun createVideo() {
        CoroutineScope(Dispatchers.IO).launch {
            startRecording()
            for (i in 0 until bufferFrameCount) {
                frameProcessor.getMediaPlayerFrame(mediaPlayerFrameList[i]).let {
                    videoMaker?.encodeFrame(it, i)
                }
            }
            stopRecording()
        }
    }

    private fun startRecording() {
        videoMaker = VideoMaker(
            context = context,
            width = imgWidth,
            height = imgHeight,
            bitRate = 6000000,
            frameRate = videoFrameRate
        )
        videoMaker?.startRecording()
    }

    private fun stopRecording() {
        videoMaker?.stopRecording()
        videoMaker = null
    }

    fun clearMediaPlayerBuffer() {
        frameProcessor.clearMediaPlayerBuffer()
    }

    fun getBufferFrameCount(): Int {
        return bufferFrameCount
    }

    fun getFrameAtIndex(index: Int): ImageBitmap {
        return frameProcessor.getMediaPlayerFrame(mediaPlayerFrameList[index])
            .toBitmap().asImageBitmap()
    }

    fun goToFrame(index: Int) {
        frameIndex = index
        loadFrame(frameIndex)
    }

    fun getSliderPosition(): Float {
        return frameIndex.toFloat()
    }
}