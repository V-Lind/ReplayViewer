package main.src.replayviewer.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import main.src.replayviewer.util.deleteContentsRecursively
import main.src.replayviewer.util.deleteFile
import main.src.replayviewer.util.loadFromDisk
import main.src.replayviewer.util.makeDirectory
import main.src.replayviewer.util.rotate
import main.src.replayviewer.util.saveToDisk
import main.src.replayviewer.util.toBitmap
import main.src.replayviewer.util.toJpeg
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

class FrameProcessor(
    context: Context,
    private var preferences: CameraConfiguration,
    private val _realtimeFrameEmitter: MutableStateFlow<ImageBitmap?>,
    private val _delayedFrameEmitter: MutableStateFlow<ImageBitmap?>,
    private val userInRealtimeViewer: MutableState<Boolean>,
    private val userInDelayViewer: MutableState<Boolean>,
    private val _bufferState: MutableStateFlow<Pair<Int, Int>>

) {
    init {
        Log.d("FrameProcessor", "Initializing Frame Processor")
    }

    private var bufferSize = preferences.delayLength * preferences.frameRate
    private var mediaPlayerBufferSize = preferences.mediaPlayerClipLength * preferences.frameRate

    // Make buffer for stream frames
    private var streamBuffer = FrameStorageBuffer(bufferSize)

    // Directory where this buffer saves its frames
    private val streamBufferDirectory = makeDirectory("streamFrames", context)


    // Make buffer for media player frames
    private var mediaPlayerBuffer = FrameStorageBuffer(mediaPlayerBufferSize)

    private val mediaPlayerFileDirectory = makeDirectory("mediaPlayerFrames", context)

    private var isFileTransferActive = false
    private var filesToDeleteQueue = ConcurrentLinkedQueue<String>()

    private var frameMemorySize: Long? = null

    private var scopeIO = CoroutineScope(Dispatchers.IO)
    private var scopeDefault = CoroutineScope(Dispatchers.Default)

    fun cycleFrames(bitmap: Bitmap, frameNumber: Int, imageOrientation: Int) {
        scopeDefault.launch {
            val rotatedBitmap = bitmap.rotate(imageOrientation.toFloat())

            val byteArray = rotatedBitmap.toJpeg()

            // Get size of a single jpeg frame
            frameMemorySize = frameMemorySize ?: byteArray.size.toLong()

            bitmap.recycle()

            val savedFilePath = saveToDisk(byteArray, streamBufferDirectory, "$frameNumber")

            scopeIO.launch {
                if (streamBuffer.isBufferFull()) {
                    val filePath = streamBuffer.removeFirst()
                    if (userInDelayViewer.value) {
                        handleDelayedFrameEmission(filePath)
                    }
                    if (mediaPlayerBuffer.isBufferFull()) {
                        val mediaPlayerFramePath = mediaPlayerBuffer.removeFirst()
                        handleDeletions(mediaPlayerFramePath)
                    }
                    mediaPlayerBuffer.addLast(filePath)
                } else {
                    _bufferState.emit(Pair(streamBuffer.getFrameCount(), bufferSize))
                }
            }

            if (userInRealtimeViewer.value) {
                handleRealtimeFrameEmission(rotatedBitmap)
            }

            savedFilePath?.let { streamBuffer.addLastOrdered(frameNumber, it) }
        }
    }

    private fun handleDelayedFrameEmission(filePath: String) {
        val delayedFrame = loadFromDisk(filePath)
        emitDelayedFrame(delayedFrame)
    }

    private fun handleRealtimeFrameEmission(rotatedBitmap: Bitmap) {
        scopeIO.launch {
            _realtimeFrameEmitter.emit(rotatedBitmap.asImageBitmap())
        }
    }

    private fun handleDeletions(mediaPlayerFramePath: String) {
        try {
            if (isFileTransferActive) {
                // Hold onto files until transfer is complete
                filesToDeleteQueue.add(mediaPlayerFramePath)
            } else {
                // Delete file when transfer not active
                deleteFile(mediaPlayerFramePath)

                // Delete remainder files from queue after transfer is complete
                with(filesToDeleteQueue) {
                    if (this.isNotEmpty()) {
                        forEach {
                            deleteFile(it)
                            remove(it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FrameProcessor", "Error deleting media player frame: $e")
            Log.e("FrameProcessor", "Error trying to delete: $mediaPlayerFramePath")
        }
    }

    private fun emitDelayedFrame(byteArray: ByteArray) {
        scopeIO.launch {
            val bitmap = byteArray.toBitmap().asImageBitmap()
            _delayedFrameEmitter.emit(bitmap)
        }
    }

    fun getMediaPlayerFrameCount(): Int {
        return mediaPlayerBuffer.getFrameCount()
    }

    fun getMediaPlayerFiles(): List<String> {
        var frameList: List<String>
        synchronized(mediaPlayerBuffer) {
            isFileTransferActive = true
            frameList = mediaPlayerBuffer.toList().toMutableList()
        }

        val existingFiles = mediaPlayerFileDirectory.listFiles()?.map { it.absolutePath } ?: emptyList()

        // Schedule files to be deleted 20 seconds from now to allow video creation to finish
        CoroutineScope(Dispatchers.IO).launch {
            delay(20000)
            existingFiles.forEach { filePath ->
                if (!frameList.contains(filePath)) {
                    deleteFile(filePath)
                }
            }
            Log.d("CustomMediaPlayer", "Finished deleting unnecessary mediaplayer files")
        }

        frameList = frameList.map {
            File(it).copyTo(File(mediaPlayerFileDirectory, File(it).name), true).absolutePath
        }

        isFileTransferActive = false

        return frameList
    }


    fun getFrame(filePath: String): ByteArray {
        return loadFromDisk(filePath)
    }

    fun restartFrameProcessor(activeCameraConfiguration: CameraConfiguration) {

        preferences = activeCameraConfiguration

        // Clear values from previous configuration
        bufferSize = activeCameraConfiguration.delayLength * activeCameraConfiguration.frameRate
        mediaPlayerBufferSize = activeCameraConfiguration.mediaPlayerClipLength * activeCameraConfiguration.frameRate
        frameMemorySize = null

        // Cancel previous scopes and create new ones
        scopeIO.cancel()
        scopeDefault.cancel()
        scopeIO = CoroutineScope(Dispatchers.IO)
        scopeDefault = CoroutineScope(Dispatchers.Default)
        Log.d("FrameProcessor", "Reseting Frame Processor")

        // Reset buffers
        streamBuffer.reset()
        mediaPlayerBuffer.reset()

        // Reset delayed frame image
        CoroutineScope(Dispatchers.Default).launch {
            _delayedFrameEmitter.emit(null)
        }

        // Set new bufferSizes according to active preference
        streamBuffer.reconfigure(bufferSize = activeCameraConfiguration.delayLength * activeCameraConfiguration.frameRate)
        mediaPlayerBuffer.reconfigure(bufferSize = activeCameraConfiguration.mediaPlayerClipLength * activeCameraConfiguration.frameRate)

        // Clear any remaining files from previous setup
        deleteContentsRecursively(streamBufferDirectory)
        deleteContentsRecursively(mediaPlayerFileDirectory)
        Log.d("FrameProcessor", "Frame Processor Reseted")
    }

    fun getFrameMemorySize(): Int? {
        Log.d("FrameProcessor", "Frame memory size: $frameMemorySize")
        return frameMemorySize?.toInt()
    }

}