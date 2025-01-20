package main.src.replayviewer.model

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.StateFlow
import main.src.replayviewer.util.resolutionStringToSize


class DelayViewerViewModel(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
) : ViewModel() {


    private var preferenceRepository = PreferenceRepository(context)
    val preferences: StateFlow<List<RecordingConfiguration>> = preferenceRepository.preferences

    private val mediaRepository = MediaRepository(
        context,
        preferenceRepository.getActivePreference(),
        lifecycleOwner,
        cameraProviderFuture,
        preferenceRepository.getBackCameraProperties(),
        preferenceRepository.getFrontCameraProperties()
    )


    var realtimeFrame: StateFlow<ImageBitmap?> = mediaRepository.realtimeFrame
    var delayedFrame: StateFlow<ImageBitmap?> = mediaRepository.delayedFrame
    val currentMediaPlayerFrame: StateFlow<ImageBitmap?> = mediaRepository.currentMediaPlayerFrame
    val bufferState: StateFlow<Pair<Int, Int>> = mediaRepository.bufferState
    val isCameraRunning: StateFlow<Boolean> = mediaRepository.isCameraRunning



    fun getMediaPlayer(): CustomMediaPlayer {
        return mediaRepository.getMediaPlayer()
    }

    fun initializeCamera() {
        setupCamera()
    }

    private fun setupCamera() {
        Log.d("DelayViewerViewModel", "Setting up camera")
        mediaRepository.setupCamera()
    }

    fun setShouldEmitRealtimeFrames(boolean: Boolean) {
        mediaRepository.setShouldEmitRealtimeFrames(boolean)
    }

    fun setShouldEmitDelayedFrames(boolean: Boolean) {
        mediaRepository.setShouldEmitDelayedFrames(boolean)
    }

    fun getWhichFrameBeingEmitted(): FrameType {
        return mediaRepository.getWhichFrameBeingEmitted()
    }

    fun changeActivePreference(id: Int) {
        preferenceRepository.setActivePreference(id)
        resetMediaRepository()
    }

    fun getActivePreference(needsNewDefaults: Boolean = false): RecordingConfiguration {
        return preferenceRepository.getActivePreference(needsNewDefaults)
    }

    fun resetMediaRepository(newConfig: RecordingConfiguration? = null) {
        // Stop camera/unbind use cases
        mediaRepository.stopCamera()


        Log.d("DelayViewerViewModel", "Resetting view model with new config: $newConfig")
        // Reinitialize mediaRepository with new active preference to clear all data
        newConfig?.let {
            mediaRepository.changeConfiguration(it)
        } ?: run {
            mediaRepository.changeConfiguration(preferenceRepository.getActivePreference())
        }

        realtimeFrame = mediaRepository.realtimeFrame
        delayedFrame = mediaRepository.delayedFrame

        // Restart camera setup
        setupCamera()
    }

    fun hasValidCameraConfig(): Boolean {
        preferenceRepository.getActivePreference().let {
            return it.isActive
        }
    }


    fun deletePreference(id: Int) {
        preferenceRepository.deletePreference(id)
    }

    fun createPreference(cameraConfig: RecordingConfiguration) {
        preferenceRepository.addPreference(cameraConfig)
        resetMediaRepository(cameraConfig)
    }

    fun getCameraProperties(cameraId: String): CameraProperties? {
        return when (cameraId) {
            "FRONT" -> preferenceRepository.getFrontCameraProperties()
            else -> preferenceRepository.getBackCameraProperties()
        }
    }

    fun setCameraZoomLevel(zoomLevel: Float) {
        mediaRepository.setZoomLevel(zoomLevel)
    }

    fun getAvailableDiskSpace(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        return bytesAvailable / (1024 * 1024)
    }

    fun getEstimatedDiskSpaceRequired(currentPreference: RecordingConfiguration): Long {

        val frameSize = mediaRepository.getFrameMemorySize()?.toLong() ?: 0L


        val mediaPlayerMultiplier = currentPreference.mediaPlayerClipLength * currentPreference.frameRate
        val streamBufferMultiplier = currentPreference.delayLength * currentPreference.frameRate

        val mp4RequiredSizeEstimate: Long = currentPreference.mediaPlayerClipLength.toLong() * 6000000

        val totalSize: Long = (frameSize * (mediaPlayerMultiplier + streamBufferMultiplier)) + mp4RequiredSizeEstimate
        Log.d("DelayViewerViewModel", "Total size: $totalSize")

        return totalSize / (1024 * 1024)
    }

    fun getAvailableCameraNames(): List<String> {
        return preferenceRepository.getAvailableCameraNames()
    }

    fun setCameraFocusLevel(focusLevel: Float) {
        mediaRepository.setCameraFocusLevel(focusLevel)
    }

    fun setMediaPlayerSpeed(speed: Double) {
        preferenceRepository.setMediaPlayerSpeed(speed)
    }

}

