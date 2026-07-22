package com.festerhead.cygnusplayer

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.festerhead.cygnusplayer.ui.screens.NowPlayingScreen
import com.festerhead.cygnusplayer.ui.screens.PlaylistPickerScreen
import com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme
import com.festerhead.cygnusplayer.ui.viewmodel.NowPlayingViewModel

/**
 * The main activity of the Cygnus Player application.
 *
 * Acts as the entry point, sets up the Database, handles runtime permissions,
 * and hosts the root Compose UI hierarchy.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = (application as CygnusApplication).database

        setContent {
            CygnusPlayerTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { /* Handle result */ }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                }

                val viewModel: PlaylistPickerViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return PlaylistPickerViewModel(application, db.playlistStateDao()) as T
                        }
                    },
                )

                val pickerUiState by viewModel.uiState.collectAsState()

                if (pickerUiState.activePlaylistPath != null) {
                    val nowPlayingViewModel: NowPlayingViewModel = viewModel()
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
                    PlaylistPickerScreen(viewModel = viewModel) { path ->
                        viewModel.onPlaylistClicked(this, path) {
                            // Handled by reactive state
                        }
                    }
                }
            }
        }
    }
}
