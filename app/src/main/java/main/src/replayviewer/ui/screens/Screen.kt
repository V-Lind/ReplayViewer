package main.src.replayviewer.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val name: String,
    val icon: ImageVector
) {
    data object MainMenu : Screen(
        "mainMenu",
        "Main Menu",
        Icons.Default.Home
    )

    data object RealtimeViewer : Screen(
        "realtimeViewer",
        "Realtime Viewer",
        Icons.Default.PlayArrow
    )

    data object DelayViewer : Screen(
        "delayViewer",
        "Delay Viewer",
        Icons.Default.PlayArrow
    )

    data object MediaPlayer : Screen(
        "mediaPlayer",
        "Media Player",
        Icons.Default.PlayArrow
    )

    data object Settings : Screen(
        "settings",
        "Settings",
        Icons.Default.Settings
    )

    // Add mapper for route to name
    companion object {
        val routesToNames: Map<String, String> by lazy {
            mapOf(
                MainMenu.route to MainMenu.name,
                RealtimeViewer.route to RealtimeViewer.name,
                DelayViewer.route to DelayViewer.name,
                Settings.route to Settings.name,
                MediaPlayer.route to MediaPlayer.name
            )
        }
    }
}