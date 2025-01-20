package main.src.replayviewer

import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import main.src.replayviewer.model.DelayViewerViewModel
import main.src.replayviewer.ui.screens.DelayViewerScreen
import main.src.replayviewer.ui.screens.MainMenuScreen
import main.src.replayviewer.ui.screens.MediaPlayerScreen
import main.src.replayviewer.ui.screens.RealtimeViewerScreen
import main.src.replayviewer.ui.screens.Screen
import main.src.replayviewer.ui.screens.SettingsScreen
import main.src.replayviewer.util.clearStorage

@Composable
fun ReplayViewerApp(widthSizeClass: WindowWidthSizeClass) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val viewModel: DelayViewerViewModel = viewModel(
        key = "DelayViewerViewModel",
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DelayViewerViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return DelayViewerViewModel(context, lifecycleOwner, cameraProviderFuture) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )


    // Select layout based on screen size
//    val isCompactScreen = widthSizeClass == WindowWidthSizeClass.Compact
    val isCompactScreen = true

    // Clear storage on app exit
    DisposableEffect(Unit) {
        onDispose {
            clearStorage(context)
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.hasValidCameraConfig()) {
            Log.d("ReplayViewerApp", "Has valid camera config")
            viewModel.initializeCamera()
        }
    }

    navController.addOnDestinationChangedListener { _, destination, _ ->

        with(viewModel) {
            setShouldEmitRealtimeFrames(
                destination.route == "${Screen.RealtimeViewer.route}/{fromSettings}"
                        || destination.route == Screen.RealtimeViewer.route
            )
            setShouldEmitDelayedFrames(destination.route == Screen.DelayViewer.route)
        }
    }



    return if (isCompactScreen) {
        Scaffold(
            content = { paddingValues ->
                CreateContent(
                    navController,
                    paddingValues,
                    viewModel
                )
            },
        )
    } else {
        //TODO: Add tablet layout
        ModalNavigationDrawer(
            drawerContent = {},
            drawerState = drawerState,
            content = {
                Row {

                }
            }
        )
    }
}


@Composable
private fun CreateContent(
    navController: NavHostController,
    innerPadding: PaddingValues,
    viewModel: DelayViewerViewModel,
) {

    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route,
        modifier = Modifier.padding(innerPadding),
        builder = {
            composable(Screen.MainMenu.route) { MainMenuScreen(navController, viewModel) }
            composable(Screen.RealtimeViewer.route) { RealtimeViewerScreen(viewModel) }

            // Passes the argument to trigger config creation when coming via settings
            composable("${Screen.RealtimeViewer.route}/{fromSettings}") { backStackEntry ->
                RealtimeViewerScreen(
                    viewModel = viewModel,
                    fromSettings = backStackEntry.arguments?.getString("fromSettings") == "fromSettings"
                )
            }
            composable(Screen.DelayViewer.route) { DelayViewerScreen(viewModel, navController) }
            composable(Screen.MediaPlayer.route) { MediaPlayerScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel, navController) }
        }
    )
}


