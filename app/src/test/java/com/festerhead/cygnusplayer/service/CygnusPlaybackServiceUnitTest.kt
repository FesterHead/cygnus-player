package com.festerhead.cygnusplayer.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.festerhead.cygnusplayer.CygnusApplication
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [CygnusPlaybackService.MediaLibraryCallback].
 *
 * Verifies that the service enforces a minimalist Play/Pause-only player command set
 * and provides a valid, non-browsable media library root.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = CygnusApplication::class)
class CygnusPlaybackServiceUnitTest {

    /**
     * Verifies that [CygnusPlaybackService.MediaLibraryCallback.onGetLibraryRoot]
     * returns a valid non-browsable root node (`CYGNUS_MINIMALIST_ROOT`).
     */
    @Test
    fun testMediaLibraryCallbackGetLibraryRoot() {
        val callback = CygnusPlaybackService.MediaLibraryCallback()
        val mockSession = mockk<MediaLibraryService.MediaLibrarySession>(relaxed = true)
        val mockController = mockk<MediaSession.ControllerInfo>(relaxed = true)

        val rootFuture = callback.onGetLibraryRoot(mockSession, mockController, null)
        val result = rootFuture.get()

        assertNotNull(result)
        val rootItem: MediaItem? = result.value
        assertNotNull(rootItem)
        assertEquals("CYGNUS_MINIMALIST_ROOT", rootItem?.mediaId)
        assertEquals(false, rootItem?.mediaMetadata?.isBrowsable)
        assertEquals(false, rootItem?.mediaMetadata?.isPlayable)
    }

    /**
     * Verifies that [CygnusPlaybackService.MediaLibraryCallback.onConnect] enforces the minimalist
     * player command set by removing Next, Previous, and Seeking commands, leaving only Play/Pause.
     */
    @Test
    fun testMediaLibraryCallbackEnforcesMinimalistPlayerCommands() {
        val callback = CygnusPlaybackService.MediaLibraryCallback()
        val mockSession = mockk<MediaLibraryService.MediaLibrarySession>(relaxed = true)
        val mockController = mockk<MediaSession.ControllerInfo>(relaxed = true)

        val connectionResult = callback.onConnect(mockSession, mockController)
        assertNotNull(connectionResult)

        val playerCommands = connectionResult.availablePlayerCommands
        assertTrue("Player commands must contain COMMAND_PLAY_PAUSE", playerCommands.contains(Player.COMMAND_PLAY_PAUSE))
        assertFalse("Player commands must NOT contain COMMAND_SEEK_TO_NEXT", playerCommands.contains(Player.COMMAND_SEEK_TO_NEXT))
        assertFalse("Player commands must NOT contain COMMAND_SEEK_TO_PREVIOUS", playerCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))
        assertFalse("Player commands must NOT contain COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM", playerCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
    }
}
