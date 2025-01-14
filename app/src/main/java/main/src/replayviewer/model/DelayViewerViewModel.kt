package main.src.replayviewer.model

import android.app.Application
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.StateFlow


class DelayViewerViewModel(
    application: Application,
    lifecycleOwner: LifecycleOwner,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
) : ViewModel() {


    private var preferenceRepository = PreferenceRepository(application)
    val preferences: StateFlow<List<CameraConfiguration>> = preferenceRepository.preferences

    private val mediaRepository = MediaRepository(
        application,
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

    fun toggleUserInRealtimeViewer(boolean: Boolean) {
        mediaRepository.toggleUserInRealtimeViewer(boolean)
    }

    fun toggleUserInDelayViewer(boolean: Boolean) {
        mediaRepository.toggleUserInDelayViewer(boolean)
    }

    fun changeActivePreference(id: Int) {
        preferenceRepository.setActivePreference(id)
        resetMediaRepository()
    }

    fun getActivePreference(): CameraConfiguration {
        return preferenceRepository.getActivePreference()
    }

    fun resetMediaRepository(newConfig: CameraConfiguration? = null) {
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

    fun createPreference(cameraConfig: CameraConfiguration) {
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

    fun getAvailableCameraNames(): List<String> {
        return preferenceRepository.getAvailableCameraNames()
    }

    fun setCameraFocusLevel(focusLevel: Float) {
        mediaRepository.setCameraFocusLevel(focusLevel)
    }


}

