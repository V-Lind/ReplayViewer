package main.src.replayviewer.model

import android.content.ContentValues
import android.content.Context
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import main.src.replayviewer.util.toBitmap

class VideoMaker(
    private val context: Context,
    private val width: Int,
    private val height: Int,
    private val bitRate: Int,
    private val frameRate: Int
) {
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var trackIndex = -1
    private var isMuxerStarted = false
    private var inputSurface: Surface? = null
    private var frameIndex = 0
    private var presentationTimeUs = 0L


    fun startRecording() {
        val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        mediaCodec = MediaCodec.createEncoderByType("video/avc").apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = createInputSurface()
            start()
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "DelayCapture_${System.currentTimeMillis() / 1000}")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/")
        }

        val uri: Uri? =
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            val pfd = context.contentResolver.openFileDescriptor(it, "w")!!

            mediaMuxer = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            pfd.close()
            Log.d("VideoMaker", "Video will be saved to: ${it.path}")
        }
        isMuxerStarted = false
    }

    private fun stopRecording() {
        mediaCodec?.apply {
            Log.d("VideoMaker", "Stopping recording")
            stop()
            release()
        }
        mediaCodec = null

        mediaMuxer?.apply {
            if (isMuxerStarted) {
                stop()
                release()
            }
        }
        mediaMuxer = null
        isMuxerStarted = false
    }

    private fun encodeFrame(byteArray: ByteArray?, endOfStream: Boolean = false) {
        Log.d("VideoMaker", "Start Encoding frame")
        if (byteArray != null) {
            val bitmap = byteArray.toBitmap()
            val canvas = inputSurface?.lockCanvas(null)
            canvas?.let {
                val destRect = Rect(0, 0, it.width, it.height)
                it.drawBitmap(bitmap, null, destRect, null)
                inputSurface?.unlockCanvasAndPost(it)
            }
        }

        val bufferInfo = MediaCodec.BufferInfo()

        // Add end of stream flag
        if (endOfStream) {
            Log.d("VideoMaker", "End of stream")
            mediaCodec?.signalEndOfInputStream()
        }

        var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 1000000) ?: -1
         Log.d("VideoMaker", "Output buffer index: $outputBufferIndex")
        while (outputBufferIndex >= 0) {
            val outputBuffer = mediaCodec?.getOutputBuffer(outputBufferIndex)
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                bufferInfo.size = 0
            }

            if (bufferInfo.size != 0) {
                outputBuffer?.position(bufferInfo.offset)
                outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)

                if (!isMuxerStarted) {
                    val format = mediaCodec?.outputFormat
                    trackIndex = mediaMuxer?.addTrack(format!!) ?: -1
                    mediaMuxer?.start()
                    isMuxerStarted = true
                }

                // Calculate presentation time in microseconds
                presentationTimeUs = frameIndex * 1000000L / frameRate
                Log.d("VideoMaker", "Frame index: $frameIndex | Presentation time: $presentationTimeUs")

                bufferInfo.presentationTimeUs = presentationTimeUs
                frameIndex++


                mediaMuxer?.writeSampleData(trackIndex, outputBuffer!!, bufferInfo)
            }

            mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
            outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
        }
    }

    fun createVideo(frameList: List<String>, frameProcessor: FrameProcessor) {
        var index = 1
        frameList.forEach { filePath ->
            val byteArray = frameProcessor.getFrame(filePath)
            encodeFrame(byteArray)
            Log.d("VideoMaker", "Frames encoded: $index")
            index++
        }
        // Add an additional frame for the end flag
        encodeFrame(null, true)
        Log.d("VideoMaker", "All frames processed. Total frames: ${frameList.size}")
        stopRecording()
    }
}




