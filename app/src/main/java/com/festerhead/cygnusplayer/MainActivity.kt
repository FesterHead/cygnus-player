package com.festerhead.cygnusplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.festerhead.cygnusplayer.ui.screens.NowPlayingScreen
import com.festerhead.cygnusplayer.ui.screens.PlaylistPickerScreen
import com.festerhead.cygnusplayer.ui.settings.SettingsScreen
import com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme
import com.festerhead.cygnusplayer.ui.viewmodel.NowPlayingViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Screen destinations supported by the application.
 */
enum class Screen {
    PLAYLIST_PICKER,
    NOW_PLAYING,
    SETTINGS
}

/**
 * The main activity of the Cygnus Player application.
 *
 * Acts as the entry point, sets up the Database, handles runtime permissions,
 * and hosts the root Compose UI hierarchy.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: PlaylistPickerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = (application as CygnusApplication).database
                return PlaylistPickerViewModel(application, db.playlistStateDao()) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CygnusPlayerTheme {
                var isReady by remember { mutableStateOf(value = false) }

                LaunchedEffect(Unit) {
                    // Small delay to ensure system binder is stable on Android 17.1
                    delay(500.milliseconds)
                    isReady = true
                }

                if (isReady) {
                    val pickerUiState by viewModel.uiState.collectAsState()
                    var currentScreen by remember { mutableStateOf(Screen.PLAYLIST_PICKER) }
                    var hasInitialNavigated by remember { mutableStateOf(false) }

                    // On initial launch, navigate to Now Playing if a playlist is active
                    LaunchedEffect(pickerUiState.activePlaylistPath) {
                        if (!hasInitialNavigated && pickerUiState.activePlaylistPath != null) {
                            currentScreen = Screen.NOW_PLAYING
                            hasInitialNavigated = true
                        }
                    }

                    when (currentScreen) {
                        Screen.SETTINGS -> {
                            SettingsScreen(onNavigateBack = { currentScreen = Screen.PLAYLIST_PICKER })
                        }
                        Screen.NOW_PLAYING -> {
                            val nowPlayingViewModel: NowPlayingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            LaunchedEffect(pickerUiState.activePlaylistPath, pickerUiState.activeShuffleMode) {
                                val path = pickerUiState.activePlaylistPath
                                if (path == null) {
                                    nowPlayingViewModel.clear()
                                    return@LaunchedEffect
                                }
                                val decodedPath = try {
                                    android.net.Uri.decode(path)
                                } catch (_: Exception) {
                                    path
                                }
                                val name = decodedPath.substringAfterLast("/").substringAfterLast("\\")
                                nowPlayingViewModel.initialize(
                                    name,
                                    pickerUiState.activeShuffleMode,
                                )
                            }

                            NowPlayingScreen(
                                viewModel = nowPlayingViewModel,
                                onNavigateBack = {
                                    currentScreen = Screen.PLAYLIST_PICKER
                                }
                            )
                        }
                        Screen.PLAYLIST_PICKER -> {
                            PlaylistPickerScreen(
                                viewModel = viewModel,
                                onPlaylistSelected = { path ->
                                    viewModel.onPlaylistClicked(this, path) {
                                        currentScreen = Screen.NOW_PLAYING
                                    }
                                },
                                onSettingsClicked = {
                                    currentScreen = Screen.SETTINGS
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
