package main.src.replayviewer.model

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
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
    private val bufferFrameCount = frameProcessor.getMediaPlayerFrameCount()
    private val mediaPlayerFrameList: List<String> = frameProcessor.getMediaPlayerFiles()

    private var imgWidth = 0
    private var imgHeight = 0


    init {
        loadFrame(frameIndex)
    }


    fun play() {
        if (isPlaying) return

        isPlaying = true

        // Start updating frames on playerJob
        playerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isPlaying) {
                loadFrame(frameIndex)
                frameIndex = (frameIndex + 1) % mediaPlayerFrameList.size
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

            // Load frame from disk
            val frameByteArray = frameProcessor.getFrame(filePath)

            val bitmap = frameByteArray.toBitmap().asImageBitmap()

            imgWidth = bitmap.width
            imgHeight = bitmap.height

            // Emit frame to UI
            _currentMediaPlayerFrame.emit(bitmap)

        }
    }

    fun createVideo() {
        Log.d("CustomMediaPlayer", "Creating video: ${mediaPlayerFrameList.size} frames")

        CoroutineScope(Dispatchers.IO).launch {
            val videoMaker = VideoMaker(
                context = context,
                width = imgWidth,
                height = imgHeight,
                bitRate = 6000000,
                frameRate = videoFrameRate
            )

            videoMaker.startRecording()
            videoMaker.createVideo(mediaPlayerFrameList, frameProcessor)
        }
    }



    fun getBufferFrameCount(): Int {
        return bufferFrameCount
    }

    fun goToFrame(index: Int) {
        frameIndex = index
        loadFrame(frameIndex)
    }

    fun getSliderPosition(): Float {
        return frameIndex.toFloat()
    }

    fun createImage() {
        Log.d("CustomMediaPlayer", "Creating image")
        val filePath = mediaPlayerFrameList[frameIndex]
        frameProcessor.getFrame(filePath).let { byteArray ->
            val contentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "ReplayViewer_img_${System.currentTimeMillis()}.jpg"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ReplayViewer")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it).use { outputStream ->
                    outputStream?.write(byteArray)
                }
            }
        }
    }
}