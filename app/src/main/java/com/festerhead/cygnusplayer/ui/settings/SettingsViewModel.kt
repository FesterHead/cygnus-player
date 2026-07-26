package com.festerhead.cygnusplayer.ui.settings

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.festerhead.cygnusplayer.CygnusApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Diagnostic data for the Settings screen.
 * 
 * @property totalTracks Total unique tracks indexed in the database.
 * @property totalPlaylists Total number of playlists in history.
 */
data class SettingsUiState(
    val totalTracks: Int = 0,
    val totalPlaylists: Int = 0,
)

/**
 * ViewModel for the Settings screen.
 * Handles configuration resets and diagnostic data retrieval.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadDiagnostics()
    }

    /**
     * Refreshes the database diagnostic counts.
     */
    fun loadDiagnostics() {
        viewModelScope.launch {
            val app = getApplication<CygnusApplication>()
            val tracks = app.database.trackDao().getTrackCount()
            val playlists = app.database.playlistStateDao().getPlaylistCount()
            _uiState.update { it.copy(totalTracks = tracks, totalPlaylists = playlists) }
        }
    }

    /**
     * Clears the library root URI from shared preferences, forcing a re-link.
     */
    fun resetRootFolder() {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("cygnus_prefs", Context.MODE_PRIVATE)
        prefs.edit { remove("library_root") }
    }
}
