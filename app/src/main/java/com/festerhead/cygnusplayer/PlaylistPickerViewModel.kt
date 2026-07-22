package com.festerhead.cygnusplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.festerhead.cygnusplayer.data.daos.PlaylistStateDao
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.service.CygnusPlaybackService
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Playlist Picker.
 *
 * @property history List of previously loaded playlists and their state configurations.
 * @property isLoading Indicates if the history is currently being loaded from the DB.
 * @property errorMessage Present if an operation fails (e.g., file access error).
 * @property pendingPlaylistPath The path to a newly selected playlist file that is pending user mode selection.
 * @property activePlaylistPath The path to the playlist currently being played in the service.
 * @property libraryRootUri The URI of the user-selected music root folder.
 */
data class PlaylistPickerUiState(
    val history: List<PlaylistStateEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pendingPlaylistPath: String? = null,
    val activePlaylistPath: String? = null,
    val activeShuffleMode: ShuffleMode = ShuffleMode.SEQUENTIAL,
    val libraryRootUri: String? = null,
)

/**
 * ViewModel for managing the playlist history and new file selection orchestration.
 *
 * @param application The application context.
 * @param playlistStateDao DAO for accessing stored playlist states.
 */
class PlaylistPickerViewModel(
    application: android.app.Application,
    private val playlistStateDao: PlaylistStateDao,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlaylistPickerUiState())
    val uiState: StateFlow<PlaylistPickerUiState> = _uiState.asStateFlow()

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null

    init {
        loadHistory()
        initMediaController()
    }

    private fun initMediaController() {
        val app = getApplication<android.app.Application>()
        val sessionToken = SessionToken(
            app,
            android.content.ComponentName(app, CygnusPlaybackService::class.java),
        )
        mediaControllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        mediaControllerFuture?.addListener(
            {
                val controller = mediaControllerFuture?.get()
                controller?.addListener(
                    object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            syncActivePlaylistWithService()
                        }
                    }
                )
                syncActivePlaylistWithService()
            },
            androidx.core.content.ContextCompat.getMainExecutor(app)
        )
    }

    private fun syncActivePlaylistWithService() {
        val controller = if (mediaControllerFuture?.isDone == true) mediaControllerFuture?.get() else null
        val path = controller?.currentMediaItem?.mediaMetadata?.extras?.getString(CygnusPlaybackService.EXTRA_ACTIVE_PLAYLIST_PATH)
        
        if (path != null) {
            val savedState = uiState.value.history.find { it.m3uPath == path }
            _uiState.update { it.copy(
                activePlaylistPath = path,
                activeShuffleMode = savedState?.shuffleMode ?: ShuffleMode.SEQUENTIAL
            ) }
        }
    }

    /**
     * Loads the stored library root and history.
     */
    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("cygnus_prefs", Context.MODE_PRIVATE)
        val root = prefs.getString("library_root", null)
        _uiState.update { it.copy(libraryRootUri = root) }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val history = playlistStateDao.getAllStates()
            _uiState.update { it.copy(history = history, isLoading = false) }
            syncActivePlaylistWithService() // Re-sync in case history loaded after controller
        }
    }

    /**
     * Called when the user selects a root music folder.
     */
    fun onRootFolderSelected(context: Context, uri: Uri) {
        // Take persistable permission
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        
        // Persist the root URI
        val prefs = context.getSharedPreferences("cygnus_prefs", Context.MODE_PRIVATE)
        prefs.edit { putString("library_root", uri.toString()) }
        
        _uiState.update { it.copy(libraryRootUri = uri.toString()) }
    }

    /**
     * Updates the currently active playlist path in the UI.
     */
    fun setActivePlaylist(path: String?) {
        _uiState.update { it.copy(activePlaylistPath = path) }
    }

    /**
     * Handles playlist selection, deciding whether to start playback or just navigate.
     */
    fun onPlaylistClicked(context: Context, path: String, onNavigateToNowPlaying: () -> Unit) {
        if (uiState.value.activePlaylistPath == path) {
            // Already playing this playlist, just navigate
            onNavigateToNowPlaying()
        } else {
            // New playlist selection
            val intent = Intent(context, CygnusPlaybackService::class.java).apply {
                putExtra(CygnusPlaybackService.EXTRA_PLAYLIST_PATH, path)
            }
            context.startForegroundService(intent)
            
            // Optimistically update UI
            val savedState = uiState.value.history.find { it.m3uPath == path }
            _uiState.update { it.copy(
                activePlaylistPath = path,
                activeShuffleMode = savedState?.shuffleMode ?: ShuffleMode.SEQUENTIAL
            ) }
            onNavigateToNowPlaying()
        }
    }

    /**
     * Called when a playlist URI is selected via the system file picker.
     */
    fun onPlaylistSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                // For now, we still resolve to physical path if possible, 
                // but we'll use MediaStore for playback resolution later.
                // In Scoped Storage, we might only get a display name and need to match it.
                
                val path = getPathFromUri(uri) ?: uri.toString()

                if (!path.endsWith(".m3u", ignoreCase = true) && !path.endsWith(".m3u8", ignoreCase = true)) {
                    _uiState.update { it.copy(errorMessage = "Unsupported format. M3U/M3U8 only.") }
                    return@launch
                }

                // Check if already in history
                val existingState = playlistStateDao.getStateForPlaylist(path)
                if (existingState != null) {
                    playlistStateDao.saveState(existingState.copy(lastOpened = System.currentTimeMillis()))
                    loadHistory()
                } else {
                    _uiState.update { it.copy(pendingPlaylistPath = path) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to open playlist: ${e.localizedMessage}") }
            }
        }
    }

    /**
     * Finalizes the addition of a new playlist with the user-selected mode and saves to DB.
     */
    fun mintPlaylist(mode: ShuffleMode) {
        val path = uiState.value.pendingPlaylistPath ?: return
        viewModelScope.launch {
            val newState = PlaylistStateEntity(
                m3uPath = path,
                lastQueueId = 0,
                shuffleMode = mode,
                lastOpened = System.currentTimeMillis(),
                mapping = null,
            )
            playlistStateDao.saveState(newState)
            _uiState.update { it.copy(pendingPlaylistPath = null) }
            loadHistory()
        }
    }

    fun cancelMinting() {
        _uiState.update { it.copy(pendingPlaylistPath = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun deleteFromHistory(context: Context, state: PlaylistStateEntity) {
        viewModelScope.launch {
            // Delete from history state table first
            playlistStateDao.deleteState(state.m3uPath)
            
            // Clear queue and tracks completely via application's PlaylistRepository singleton
            val app = context.applicationContext as CygnusApplication
            app.playlistRepository.deletePlaylistData(state.m3uPath)

            // If we deleted the active playlist, clear it from UI
            if (uiState.value.activePlaylistPath == state.m3uPath) {
                _uiState.update { it.copy(activePlaylistPath = null) }
            }

            loadHistory()
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        
        // Try to get physical path (fails on modern Android for non-media files usually)
        // But for M3U8 in public folders, it might work on emulator.
        // On phone, we'll likely just store the URI string as the path.
        return null 
    }

    override fun onCleared() {
        super.onCleared()
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
