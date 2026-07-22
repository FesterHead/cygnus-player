package com.festerhead.cygnusplayer.ui.viewmodel

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.festerhead.cygnusplayer.CygnusApplication
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.service.CygnusPlaybackService

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * UI State for the Now Playing screen.
 */
data class NowPlayingUiState(
    val isPlaying: Boolean = false,
    val trackTitle: String = "No track playing",
    val albumName: String = "No album",
    val playlistName: String = "",
    val position: String = "0/0",
    val shuffleMode: ShuffleMode = ShuffleMode.SEQUENTIAL,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val artwork: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NowPlayingUiState

        if (isPlaying != other.isPlaying) return false
        if (trackTitle != other.trackTitle) return false
        if (albumName != other.albumName) return false
        if (playlistName != other.playlistName) return false
        if (position != other.position) return false
        if (shuffleMode != other.shuffleMode) return false
        if (currentPositionMs != other.currentPositionMs) return false
        if (durationMs != other.durationMs) return false
        if (artwork != null) {
            if (other.artwork == null) return false
            if (!artwork.contentEquals(other.artwork)) return false
        } else if (other.artwork != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isPlaying.hashCode()
        result = (31 * result) + trackTitle.hashCode()
        result = (31 * result) + albumName.hashCode()
        result = (31 * result) + playlistName.hashCode()
        result = (31 * result) + position.hashCode()
        result = (31 * result) + shuffleMode.hashCode()
        result = (31 * result) + currentPositionMs.hashCode()
        result = (31 * result) + durationMs.hashCode()
        result = (31 * result) + (artwork?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * ViewModel for the Now Playing screen.
 * Observes the playback service state and provides it to the UI.
 */
class NowPlayingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private var mediaControllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var progressJob: Job? = null

    init {
        val app = getApplication<Application>()
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
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _uiState.update { it.copy(isPlaying = isPlaying) }
                            if (isPlaying) {
                                startProgressTracking()
                            } else {
                                stopProgressTracking()
                            }
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            updateMetadataFromController()
                        }

                        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                            updateMetadataFromController()
                        }

                        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                            updateMetadataFromController()
                        }
                    },
                )
                val isPlaying = controller?.isPlaying ?: false
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracking()
                } else {
                    stopProgressTracking()
                }
                updateMetadataFromController()
            },
            ContextCompat.getMainExecutor(app),
        )
    }

    private val mediaController: MediaController?
        get() = if (mediaControllerFuture?.isDone == true) mediaControllerFuture?.get() else null

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if ((controller != null) && controller.isPlaying) {
                    val currentPos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.coerceAtLeast(0L)
                    _uiState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = if (dur == androidx.media3.common.C.TIME_UNSET) 0L else dur,
                        )
                    }
                }
                delay(1.seconds) // Wait 1 second between position updates
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * Updates the UI State by extracting information from the current controller.
     */
    private fun updateMetadataFromController() {
        val controller = mediaController ?: return
        val currentMediaItem = controller.currentMediaItem
        val metadata = currentMediaItem?.mediaMetadata
        
        val app = getApplication<Application>() as CygnusApplication
        val positionStr = app.queueController.getPositionString()

        val currentPos = controller.currentPosition.coerceAtLeast(0L)
        val dur = controller.duration.coerceAtLeast(0L)

        _uiState.update {
            it.copy(
                isPlaying = controller.isPlaying,
                trackTitle = metadata?.title?.toString() ?: currentMediaItem?.requestMetadata?.mediaUri?.lastPathSegment ?: "No track playing",
                albumName = metadata?.albumTitle?.toString() ?: "No album",
                position = positionStr,
                currentPositionMs = currentPos,
                durationMs = if (dur == androidx.media3.common.C.TIME_UNSET) 0L else dur,
                artwork = metadata?.artworkData,
            )
        }
    }

    /**
     * Initializes the Now Playing state with the active playlist information.
     */
    fun initialize(playlistName: String, shuffleMode: ShuffleMode) {
        // Instantly reset the UI state to a loading state to clear previous metadata leakage
        _uiState.update {
            it.copy(
                isPlaying = false,
                trackTitle = "Loading playlist...",
                albumName = "Resolving queue...",
                playlistName = playlistName,
                position = "0/0",
                shuffleMode = shuffleMode,
                currentPositionMs = 0L,
                durationMs = 0L,
            )
        }
        updateMetadataFromController()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
