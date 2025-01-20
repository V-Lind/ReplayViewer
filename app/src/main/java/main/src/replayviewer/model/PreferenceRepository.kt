package main.src.replayviewer.model

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import main.src.replayviewer.util.resolutionStringToSize
import java.io.File

class PreferenceRepository(private val context: Context) {

    private var preferencesFile = File(context.filesDir, "preferences.json")

    private val _preferences = MutableStateFlow<List<RecordingConfiguration>>(emptyList())
    val preferences: StateFlow<List<RecordingConfiguration>> = _preferences

    private var backCameraProperties: CameraProperties? = null
    private var frontCameraProperties: CameraProperties? = null

    init {
        getCameraCapabilities(context)
        //    resetPreferences()
        // load preferences from disk
        loadPreferences()
        Log.d("PreferenceRepository", "Preferences init finished: ${preferences.value}")
    }


    private fun resetPreferences() {
        // Clean out preferencesFile and make a new one
        preferencesFile.delete()
        preferencesFile = File(context.filesDir, "preferences.json")
    }

    fun getBackCameraProperties(): CameraProperties? {
        return backCameraProperties
    }

    fun getFrontCameraProperties(): CameraProperties? {
        return frontCameraProperties
    }

    private fun initializeDefaultPreferences() {

        val backRecordingConfiguration = backCameraProperties?.let {
            RecordingConfiguration(
                id = 1,
                isActive = true,
                name = "Default Back preset",
                description = "Default preset 1",
                delayLength = 10,
                mediaPlayerClipLength = 5,
                zoomLevel = 0.5f,
                isAutofocusEnabled = true,
                focusLevel = 0.5f,
                frameRate = getDefaultFrameRate(it.fpsOptions),
                cameraId = "BACK",
                resolution = getDefaultResolution(it.resolutionOptions),
                cameraOrientation = it.cameraOrientation
            )
        }

        val frontRecordingConfiguration = frontCameraProperties?.let {
            RecordingConfiguration(
                id = 2,
                isActive = false,
                name = "Default Front preset",
                description = "Default preset 2",
                delayLength = 5,
                mediaPlayerClipLength = 5,
                zoomLevel = 0.5f,
                isAutofocusEnabled = true,
                focusLevel = 0.5f,
                frameRate = getDefaultFrameRate(it.fpsOptions),
                cameraId = "FRONT",
                resolution = getDefaultResolution(it.resolutionOptions),
                cameraOrientation = it.cameraOrientation
            )
        }

        Log.d("PreferenceRepository", "Initializing default preferences")
        savePreferences(listOfNotNull(backRecordingConfiguration, frontRecordingConfiguration))
    }


    private fun savePreferences(recordingConfigurationList: List<RecordingConfiguration>) {
        if (recordingConfigurationList.all { it.isValid() }) {
            _preferences.value = recordingConfigurationList
            preferencesFile.writeText(
                Json.encodeToString(
                    ListSerializer(RecordingConfiguration.serializer()),
                    recordingConfigurationList
                )
            )
        } else {
            Log.e("PreferenceRepository", "Invalid preferences, not saving to disk")
        }
    }


    private fun loadPreferences(): List<RecordingConfiguration> {
        var result: List<RecordingConfiguration>? = null

        while(result == null) {
            if (preferencesFile.exists()) {
                val jsonString = preferencesFile.readText()
                Log.d("PreferenceRepository", "Loaded preferences from disk: $jsonString")
                result = Json.decodeFromString(ListSerializer(RecordingConfiguration.serializer()), jsonString)
            } else {
                initializeDefaultPreferences()
            }
        }

        savePreferences(result)
        return result
    }

    fun setActivePreference(id: Int) {
        updatePreferences {
            it.map { preference ->
                preference.copy(isActive = preference.id == id)
            }
        }
    }

    fun addPreference(recordingConfiguration: RecordingConfiguration) {
        // Get new max id
        val newId = (preferences.value.maxOfOrNull { it.id } ?: 0) + 1

        updatePreferences {
            it + recordingConfiguration.copy(id = newId)
        }
        // Set this as active preference
        setActivePreference(newId)
    }

    fun deletePreference(id: Int) {
        updatePreferences { it.filter { preference -> preference.id != id } }
    }

    private fun updatePreferences(transform: (List<RecordingConfiguration>) -> List<RecordingConfiguration>) {
        val currentPreferences = preferences.value
        val updatedPreferences = transform(currentPreferences)
        savePreferences(updatedPreferences)
    }

    fun getActivePreference(
        needsNewDefaults: Boolean = false
    ): RecordingConfiguration {
        var preferenceToReturn: RecordingConfiguration? = null

        val currentActivePreference = preferences.value.first { it.isActive }



        if (needsNewDefaults) {
            Log.d("PreferenceRepository", "Getting new defaults")
            val currentResolution = resolutionStringToSize(currentActivePreference.resolution)

            var newFrameRate = currentActivePreference.frameRate
            var newResolution = currentActivePreference.resolution

            val needsNewFrameRate = currentActivePreference.frameRate > 30
            val needsNewResolution = currentResolution.width * currentResolution.height >= 1920 * 1080


            val cameraToUse = when(currentActivePreference.cameraId) {
                "BACK" -> backCameraProperties!!
                else -> frontCameraProperties!!
            }


            if(needsNewFrameRate) {
                newFrameRate = getDefaultFrameRate(cameraToUse.fpsOptions)
            }

            if(needsNewResolution) {
                newResolution = getDefaultResolution(cameraToUse.resolutionOptions)
            }

            preferenceToReturn = currentActivePreference.copy(
                frameRate = if(needsNewFrameRate) newFrameRate else currentActivePreference.frameRate,
                resolution = if(needsNewResolution) newResolution else currentActivePreference.resolution
            )

        }
        Log.d("PreferenceRepository", "Returning preference: $preferenceToReturn")
        return preferenceToReturn ?: currentActivePreference
    }

    private fun getDefaultFrameRate(fpsOptions: List<Int>): Int {
        // Select 30 as default if possible on device
        val defaultFrameRate = fpsOptions.find { it == 30 } ?: fpsOptions.first()
        return defaultFrameRate
    }

    private fun getDefaultResolution(resolutions: List<Size>): String {
        val targetAspectRatio = 16f / 9f

        // Find the smallest 16:9 aspect ratio resolution
        val smallestResolution = resolutions
            .filter { it.width.toFloat() / it.height == targetAspectRatio }
            .minByOrNull { it.width * it.height }

        return smallestResolution?.let { "${it.width}x${it.height}" }
            ?: "${resolutions.last().width}x${resolutions.last().height}"
    }

    private fun getCameraCapabilities(context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val maxFocalLength =
                    characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        ?.maxOrNull()
                Log.d(
                    "CameraCapabilities",
                    "Camera ${
                        characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                            ?.toList()
                    }"
                )

                val fpsRanges: List<Int>? =
                    characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                        ?.filter { range -> range.upper == range.lower }
                        ?.map { range -> range.upper }


                Log.d("CameraCapabilities", "FPS ranges: $fpsRanges")

                val resolutions =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        ?.getOutputSizes(35)?.toList()

                val cameraRotation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

                val cameraProperties = CameraProperties(
                    cameraId,
                    fpsRanges!!,
                    resolutions!!,
                    cameraRotation!!,
                    maxFocalLength!!
                )

                backCameraProperties = backCameraProperties
                    ?: cameraProperties.takeIf { lensFacing == CameraCharacteristics.LENS_FACING_BACK }
                frontCameraProperties = frontCameraProperties
                    ?: cameraProperties.takeIf { lensFacing == CameraCharacteristics.LENS_FACING_FRONT }


            }
        } catch (e: Exception) {
            Log.e("CameraCapabilities", "Error getting camera capabilities", e)
        }
    }

    fun getAvailableCameraNames(): List<String> {
        return listOfNotNull(
            backCameraProperties?.let { "BACK" },
            frontCameraProperties?.let { "FRONT" }
        )
    }

    fun setMediaPlayerSpeed(speed: Double) {
        updatePreferences { preferences ->
            preferences.map { preference ->
                if (preference.isActive) preference.copy(mediaPlayerSpeed = speed) else preference
            }
        }
    }
}

data class CameraProperties(
    val cameraId: String,
    val fpsOptions: List<Int>,
    val resolutionOptions: List<Size>,
    val cameraOrientation: Int,
    val maxFocalLength: Float
)