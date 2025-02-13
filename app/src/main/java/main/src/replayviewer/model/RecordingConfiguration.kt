package main.src.replayviewer.model

import kotlinx.serialization.Serializable

@Serializable
data class RecordingConfiguration(
    var id: Int,
    var cameraId: String,
    var isActive: Boolean = false,
    var name: String,
    var description: String,
    var delayLength: Int,
    var mediaPlayerClipLength: Int,
    var zoomLevel: Float,
    var isAutofocusEnabled: Boolean,
    var focusLevel: Float,
    var frameRate: Int,
    var resolution: String,
    var cameraOrientation: Int,
    var mediaPlayerSpeed: Double = 1.0,
    var automaticallyMakeVideoOnOpenMediaPlayer: Boolean = false,
    var automaticallySwitchToRealTimeStream: Boolean = false,
) {


    //TODO: Update validations
    fun isValid(): Boolean {
//        return name.isNotBlank() &&
//                // Delay length should be greater than 0
//                delayLength > 0 &&
//                // Media player clip length should be greater than 0
//                mediaPlayerClipLength > 0 &&
//                // Zoom level should be between 0.0 and 1.0
//                zoomLevel in 0.0f..1.0f &&
//                // Frame rate should be greater than 0
//                frameRate > 0 &&
//                // Camera should be "BACK" or "FRONT"
//                cameraId.isNotBlank() &&
//                // Resolution should be in the format "width x height"
//                resolution.matches(Regex("\\d+x\\d+")) &&
//                // Camera orientation should be an Int
//                cameraOrientation in listOf(0, 90, 180, 270)
        return true
    }
}