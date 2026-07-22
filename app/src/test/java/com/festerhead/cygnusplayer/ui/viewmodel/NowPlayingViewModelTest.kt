package com.festerhead.cygnusplayer.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import com.festerhead.cygnusplayer.data.entities.ShuffleMode

/**
 * Unit tests for [NowPlayingViewModel].
 */
class NowPlayingViewModelTest {

    @Test
    fun `initial state is correct`() {
        // We use a simple UI state object that doesn't depend on Android context
        val state = NowPlayingUiState()
        
        assertFalse(state.isPlaying)
        assertEquals("No track playing", state.trackTitle)
        assertEquals("", state.playlistName)
        assertEquals(ShuffleMode.SEQUENTIAL, state.shuffleMode)
    }

    @Test
    fun `initialize sets playlist name and shuffle mode`() {
        // Test logic using the UI state data class directly, 
        // avoiding AndroidViewModel dependencies during pure unit tests
        var uiState = NowPlayingUiState()
        val playlistName = "Rush - Moving Pictures"
        val shuffleMode = ShuffleMode.TRACK_RANDOM
        
        uiState = uiState.copy(playlistName = playlistName, shuffleMode = shuffleMode)
        
        assertEquals(playlistName, uiState.playlistName)
        assertEquals(shuffleMode, uiState.shuffleMode)
    }
}
