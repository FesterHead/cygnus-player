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
                    var showSettings by remember { mutableStateOf(value = false) }

                    if (showSettings) {
                        SettingsScreen(onNavigateBack = { showSettings = false })
                    } else if (pickerUiState.activePlaylistPath != null) {
                        val nowPlayingViewModel: NowPlayingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        LaunchedEffect(pickerUiState.activePlaylistPath, pickerUiState.activeShuffleMode) {
                            val path = pickerUiState.activePlaylistPath!!
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
                        ) {
                            viewModel.setActivePlaylist(null)
                        }
                    } else {
                        PlaylistPickerScreen(
                            viewModel = viewModel,
                            onPlaylistSelected = { path ->
                                viewModel.onPlaylistClicked(this, path) {
                                    // Handled by reactive state
                                }
                            },
                        ) { showSettings = true }
                    }
                }
            }
        }
    }
}
