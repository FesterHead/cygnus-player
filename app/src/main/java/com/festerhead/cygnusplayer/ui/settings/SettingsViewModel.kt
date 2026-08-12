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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Diagnostic data and configuration state for the Settings screen.
 * 
 * @property totalTracks Total unique tracks indexed in the database.
 * @property totalPlaylists Total number of playlists in history.
 * @property musicRootFolder User-friendly display name of the configured music root folder.
 */
data class SettingsUiState(
    val totalTracks: Int = 0,
    val totalPlaylists: Int = 0,
    val musicRootFolder: String? = null,
)

/**
 * ViewModel for the Settings screen.
 * Handles configuration resets and diagnostic data retrieval.
 *
 * @param application The application context.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Refreshes settings configuration state and database diagnostic counts.
     */
    fun loadSettings() {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("cygnus_prefs", Context.MODE_PRIVATE)
        val rawRoot = prefs.getString("library_root", null)
        val formattedRoot = formatMusicRootFolder(rawRoot)

        viewModelScope.launch {
            val cygnusApp = getApplication<CygnusApplication>()
            val tracks = cygnusApp.database.trackDao().getTrackCount()
            val playlists = cygnusApp.database.playlistStateDao().getPlaylistCount()
            _uiState.update {
                it.copy(
                    totalTracks = tracks,
                    totalPlaylists = playlists,
                    musicRootFolder = formattedRoot
                )
            }
        }
    }

    /**
     * Clears the library root URI from shared preferences, forcing a re-link.
     */
    fun resetRootFolder() {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("cygnus_prefs", Context.MODE_PRIVATE)
        prefs.edit { remove("library_root") }
        _uiState.update { it.copy(musicRootFolder = null) }
    }

    companion object {
        /**
         * Formats a raw storage URI or file path string into a clean, readable display string.
         *
         * @param rawUriString Raw URI string stored in SharedPreferences (e.g. SAF DocumentTree URI).
         * @return Formatted human-readable path string, or null if [rawUriString] is null or blank.
         */
        fun formatMusicRootFolder(rawUriString: String?): String? {
            if (rawUriString.isNullOrBlank()) return null
            return try {
                val decoded = URLDecoder.decode(rawUriString, StandardCharsets.UTF_8.name())
                when {
                    rawUriString.contains("/tree/") -> {
                        val treeSegment = rawUriString.substringAfter("/tree/").substringBefore("?").substringBefore("#")
                        val decodedTree = URLDecoder.decode(treeSegment, StandardCharsets.UTF_8.name())
                        when {
                            decodedTree.startsWith("primary:") -> decodedTree.replaceFirst("primary:", "Internal Storage > ")
                            decodedTree.startsWith("raw:") -> decodedTree.removePrefix("raw:")
                            else -> decodedTree
                        }
                    }
                    rawUriString.startsWith("file://") -> {
                        decoded.removePrefix("file://")
                    }
                    else -> decoded
                }
            } catch (_: Exception) {
                rawUriString
            }
        }
    }
}

