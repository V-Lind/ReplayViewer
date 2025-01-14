package main.src.replayviewer

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import main.src.replayviewer.ui.theme.ReplayViewerTheme
import main.src.replayviewer.util.clearStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Clear storage on startup to get rid of any leftovers
        clearStorage(application)

        // Request permissions if not granted previously
        if (hasNecessaryAndroidPermissions()) {
            launchApp()
        } else {
            permissionResultLauncher.launch(androidPermissions)
        }
    }

    //Necessary Android permissions
    private val androidPermissions: Array<String> = mutableListOf(
        Manifest.permission.CAMERA
    ).apply {
        // Use old permissions if SDK too low
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private val permissionResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { androidPermissions ->
            val granted = androidPermissions.entries.all { it.value }
            if (granted) {
                launchApp()
            } else {
                Toast.makeText(
                    this,
                    "Permissions denied. This App cannot run without them. Shutting down.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    // Check if necessary Android permissions are granted
    private fun hasNecessaryAndroidPermissions(): Boolean {
        for (permission in androidPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    // Launches the application
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun launchApp() {
        enableEdgeToEdge()
        setContent {
            ReplayViewerTheme {
                // Check device size class
                val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
                ReplayViewerApp(widthSizeClass)
            }
        }
    }

}


