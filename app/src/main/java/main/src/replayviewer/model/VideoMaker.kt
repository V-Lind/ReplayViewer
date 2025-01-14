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

    fun stopRecording() {
        mediaCodec?.apply {
            signalEndOfInputStream()
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

    fun encodeFrame(byteArray: ByteArray, frameIndex: Int) {
        val bitmap = byteArray.toBitmap()
        val canvas = inputSurface?.lockCanvas(null)
        canvas?.let {
            val destRect = Rect(0, 0, it.width, it.height)
            it.drawBitmap(bitmap, null, destRect, null)
            inputSurface?.unlockCanvasAndPost(it)
        }

        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
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
                val presentationTimeUs = (frameIndex * 1000000L / frameRate)
                bufferInfo.presentationTimeUs = presentationTimeUs

                mediaMuxer?.writeSampleData(trackIndex, outputBuffer!!, bufferInfo)
            }

            mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
            outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
        }
    }
}