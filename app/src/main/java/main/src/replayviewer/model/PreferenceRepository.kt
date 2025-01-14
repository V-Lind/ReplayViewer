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
import java.io.File

class PreferenceRepository(private val context: Context) {

    private var preferencesFile = File(context.filesDir, "preferences.json")

    private val _preferences = MutableStateFlow<List<CameraConfiguration>>(emptyList())
    val preferences: StateFlow<List<CameraConfiguration>> = _preferences

    private var backCameraProperties: CameraProperties? = null
    private var frontCameraProperties: CameraProperties? = null

    init {
        getCameraCapabilities(context)
        //    resetPreferences()

        // load preferences from disk
        loadPreferences()?.let { savePreferences(it) } ?: run { initializeDefaultPreferences() }
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

        val backCameraConfiguration = backCameraProperties?.let {
            CameraConfiguration(
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

        val frontCameraConfiguration = frontCameraProperties?.let {
            CameraConfiguration(
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
        savePreferences(listOfNotNull(backCameraConfiguration, frontCameraConfiguration))
    }


    private fun savePreferences(cameraConfigurationList: List<CameraConfiguration>) {
        if (cameraConfigurationList.all { it.isValid() }) {
            _preferences.value = cameraConfigurationList
            preferencesFile.writeText(
                Json.encodeToString(
                    ListSerializer(CameraConfiguration.serializer()),
                    cameraConfigurationList
                )
            )
        } else {
            Log.e("PreferenceRepository", "Invalid preferences, not saving to disk")
        }
    }


    private fun loadPreferences(): List<CameraConfiguration>? {
        return if (preferencesFile.exists()) {
            val jsonString = preferencesFile.readText()
            Json.decodeFromString(ListSerializer(CameraConfiguration.serializer()), jsonString)
        } else {
            null
        }
    }

    fun setActivePreference(id: Int) {
        updatePreferences {
            it.map { preference ->
                preference.copy(isActive = preference.id == id)
            }
        }
    }

    fun addPreference(cameraConfiguration: CameraConfiguration) {
        // Get new max id
        val newId = (preferences.value.maxOfOrNull { it.id } ?: 0) + 1

        updatePreferences {
            it + cameraConfiguration.copy(id = newId)
        }
        // Set this as active preference
        setActivePreference(newId)
    }

    fun deletePreference(id: Int) {
        updatePreferences { it.filter { preference -> preference.id != id } }
    }

    private fun updatePreferences(transform: (List<CameraConfiguration>) -> List<CameraConfiguration>) {
        val currentPreferences = preferences.value
        val updatedPreferences = transform(currentPreferences)
        savePreferences(updatedPreferences)
    }

    fun getActivePreference(): CameraConfiguration {
        return preferences.value.first { it.isActive }
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
                val maxFocalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.maxOrNull()
                Log.d("CameraCapabilities", "Camera ${characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList()}")

                val fpsRanges: List<Int>? = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
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
}


data class CameraProperties(
    val cameraId: String,
    val fpsOptions: List<Int>,
    val resolutionOptions: List<Size>,
    val cameraOrientation: Int,
    val maxFocalLength: Float
)