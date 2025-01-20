package main.src.replayviewer.model

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Range
import android.util.Size
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MediaRepository(
    private val context: Context,
    private var preferences: RecordingConfiguration,
    private var lifecycleOwner: LifecycleOwner,
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    private val backCameraProperties: CameraProperties?,
    private val frontCameraProperties: CameraProperties?
) {
    init {
        Log.d("MediaRepository", "Initializing mediaRepository with prefs: $preferences")
    }

    private val _bufferState = MutableStateFlow(Pair(0, 0))
    val bufferState: StateFlow<Pair<Int, Int>> = _bufferState.asStateFlow()

    private val _realtimeFrame = MutableStateFlow<ImageBitmap?>(null)
    val realtimeFrame: StateFlow<ImageBitmap?> = _realtimeFrame.asStateFlow()

    private val _delayedFrame = MutableStateFlow<ImageBitmap?>(null)
    val delayedFrame: StateFlow<ImageBitmap?> = _delayedFrame.asStateFlow()

    private val _currentMediaPlayerFrame = MutableStateFlow<ImageBitmap?>(null)
    val currentMediaPlayerFrame: StateFlow<ImageBitmap?> = _currentMediaPlayerFrame.asStateFlow()

    private val _isCameraRunning = MutableStateFlow(false)
    val isCameraRunning: StateFlow<Boolean> = _isCameraRunning.asStateFlow()


    private val shouldEmitRealTimeFrames: MutableState<Boolean> = mutableStateOf(false)
    private val shouldEmitDelayedFrames: MutableState<Boolean> = mutableStateOf(false)

    private var camera: Camera? = null

    private val frameProcessor = FrameProcessor(
        context,
        preferences,
        _realtimeFrame,
        _delayedFrame,
        shouldEmitRealTimeFrames,
        shouldEmitDelayedFrames,
        _bufferState
    )

    fun getMediaPlayer(): CustomMediaPlayer {

        return CustomMediaPlayer(
            context = context,
            frameProcessor = frameProcessor,
            _currentMediaPlayerFrame = _currentMediaPlayerFrame,
        )
    }

    fun setShouldEmitRealtimeFrames(boolean: Boolean) {
        shouldEmitRealTimeFrames.value = boolean
    }

    fun setShouldEmitDelayedFrames(boolean: Boolean) {
        shouldEmitDelayedFrames.value = boolean
    }

    fun setZoomLevel(zoomLevel: Float) {
        camera?.cameraControl?.setLinearZoom(zoomLevel)
    }

    fun getFrameMemorySize(): Int? {
        return frameProcessor.getFrameMemorySize()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun setupCamera() {
        try {
            Log.d("MediaRepository", "Setting up camera")
            val cameraProvider = cameraProviderFuture.get()

            // Select which camera to use
            val cameraSelector = when (preferences.cameraId) {
                "FRONT" -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }

            // Select resolution to use
            val targetResolution = preferences.resolution.split("x").let {
                Size(it[0].toInt(), it[1].toInt())
            }

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        targetResolution,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
                    )
                )
                .build()


            // Set up image analysis to capture individual frames from camera
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(resolutionSelector)
                .build()

            val cameraExecutor = Executors.newSingleThreadExecutor()
            var frameNumber = 0
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val bitmap = imageProxy.toBitmap()
                    imageProxy.close()

                    frameProcessor.cycleFrames(
                        bitmap,
                        frameNumber,
                        preferences.cameraOrientation,
                        onPerformanceIssue = {
                            CoroutineScope(Dispatchers.Main).launch {
                                stopCamera()
                                Toast.makeText(
                                    context,
                                    "Camera stopped. Lower the capture resolution or fps.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                    frameNumber = (frameNumber + 1) % Int.MAX_VALUE

                } catch (e: Exception) {
                    Log.e("MediaRepository", "Error in image processing", e)
                }
            }


            // Bind camera to lifecycle
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )


            // Modify camera controls using camera2 interop
            val camera2CameraControl = Camera2CameraControl.from(camera!!.cameraControl)
            camera!!.cameraControl.setLinearZoom(preferences.zoomLevel)
            Log.d("MediaRepository", "Camera zoom level set to: ${preferences.zoomLevel}")

            val captureRequestOptionsBuilder = CaptureRequestOptions.Builder()
                // Auto exposure mode on
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                // Set auto exposure to prioritize a specific frame rate
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(preferences.frameRate, preferences.frameRate)
                )

            // Set autofocus mode based on preferences
            if (preferences.isAutofocusEnabled) {
                Log.d("MediaRepository", "Autofocus enabled")
                captureRequestOptionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
            } else {
                Log.d("MediaRepository", "Autofocus disabled")
                captureRequestOptionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                captureRequestOptionsBuilder.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    preferences.focusLevel
                )
                Log.d("MediaRepository", "Focus level: ${preferences.focusLevel}")
            }

            val captureRequestOptions = captureRequestOptionsBuilder.build()
            camera2CameraControl.captureRequestOptions = captureRequestOptions

        } catch (e: Exception) {
            Log.e("MediaRepository", "Error setting up camera", e)
        }
        CoroutineScope(Dispatchers.Main).launch {
            _isCameraRunning.emit(true)
        }
    }

    fun stopCamera() {
        cameraProviderFuture.get()?.unbindAll()
        camera = null
        CoroutineScope(Dispatchers.Main).launch {
            _isCameraRunning.emit(false)
        }
    }


    fun changeConfiguration(newRecordingConfiguration: RecordingConfiguration) {
        // Change preferences used by camera & frame processor to new active preference
        preferences = newRecordingConfiguration
        frameProcessor.restartFrameProcessor(newRecordingConfiguration)
        Log.d("MediaRepository", "Changed configuration to: $newRecordingConfiguration")
    }


    @OptIn(ExperimentalCamera2Interop::class)
    fun setCameraFocusLevel(focusLevel: Float) {
        val focusDistance = if (preferences.cameraId == "FRONT") {
            frontCameraProperties?.maxFocalLength?.times(focusLevel) ?: 0f
        } else {
            backCameraProperties?.maxFocalLength?.times(focusLevel) ?: 0f
        }

        camera?.let {
            val camera2CameraControl = Camera2CameraControl.from(it.cameraControl)
            val captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance)
                .build()
            camera2CameraControl.setCaptureRequestOptions(captureRequestOptions)
            Log.d("MediaRepository", "Camera focus level set to: $focusDistance")
        }

    }

    fun getWhichFrameBeingEmitted(): FrameType {
        return if (shouldEmitRealTimeFrames.value) FrameType.REALTIME else FrameType.DELAYED
    }

}

