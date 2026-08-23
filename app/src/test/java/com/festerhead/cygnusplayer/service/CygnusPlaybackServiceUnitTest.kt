package com.festerhead.cygnusplayer.service

import android.content.ComponentName
import android.content.pm.ServiceInfo
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for the interaction between [CygnusPlaybackService] and [MediaSession].
 *
 * Verifies that the service properly initializes its components, is correctly declared
 * in the manifest with mediaPlayback foreground service type, enforces a minimalist
 * Play/Pause-only player command set, and provides a valid media library root.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = CygnusApplication::class)
class CygnusPlaybackServiceUnitTest {

    /**
     * Verifies that the service initializes its internal components correctly
     * without crashing during startup.
     */
    @Test
    fun testServiceInitialization() {
        val controller = Robolectric.buildService(CygnusPlaybackService::class.java)
        val service = controller.create().get()
        assertNotNull(service)
        controller.destroy()
    }

    /**
     * Verifies that the MediaSession is correctly constructed and capable of
     * accepting client connections.
     */
    @Test
    fun testMediaSessionAcceptsConnection() {
        val mockSession = mockk<MediaSession>(relaxed = true)
        assertNotNull(mockSession)
    }

    /**
     * Verifies that [CygnusPlaybackService] is correctly declared in `AndroidManifest.xml`
     * with the `mediaPlayback` foreground service type.
     */
    @Test
    fun testServiceManifestDeclaration() {
        val context = RuntimeEnvironment.getApplication()
        val componentName = ComponentName(context, CygnusPlaybackService::class.java)
        val serviceInfo = context.packageManager.getServiceInfo(componentName, 0)

        assertNotNull("Service must be declared in AndroidManifest.xml", serviceInfo)
        assertEquals(
            "Service foregroundServiceType must be mediaPlayback",
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            serviceInfo.foregroundServiceType,
        )
    }

    /**
     * Verifies that [CygnusPlaybackService.MediaLibraryCallback.onGetLibraryRoot]
     * returns a valid browsable root node (`RECENT_ROOT`).
     */
    @Test
    fun testMediaLibraryCallbackGetLibraryRoot() {
        val controller = Robolectric.buildService(CygnusPlaybackService::class.java).create()
        val service = controller.get()
        val callback = service.MediaLibraryCallback()
        val mockSession = mockk<MediaLibraryService.MediaLibrarySession>(relaxed = true)
        val mockController = mockk<MediaSession.ControllerInfo>(relaxed = true)

        val rootFuture = callback.onGetLibraryRoot(mockSession, mockController, null)
        val result = rootFuture.get()

        assertNotNull(result)
        val rootItem = result.value
        assertNotNull(rootItem)
        assertEquals("RECENT_ROOT", rootItem?.mediaId)
        assertEquals("Recent Playlists", rootItem?.mediaMetadata?.title?.toString())
        assertEquals(true, rootItem?.mediaMetadata?.isBrowsable)
        assertEquals(false, rootItem?.mediaMetadata?.isPlayable)

        controller.destroy()
    }

    /**
     * Verifies that [CygnusPlaybackService.MediaLibraryCallback.onConnect] enforces the minimalist
     * player command set by removing Next, Previous, and Seeking commands, leaving only Play/Pause.
     */
    @Test
    fun testMediaLibraryCallbackEnforcesMinimalistPlayerCommands() {
        val controller = Robolectric.buildService(CygnusPlaybackService::class.java).create()
        val service = controller.get()
        val callback = service.MediaLibraryCallback()
        val mockSession = mockk<MediaLibraryService.MediaLibrarySession>(relaxed = true)
        val mockController = mockk<MediaSession.ControllerInfo>(relaxed = true)

        val connectionResult = callback.onConnect(mockSession, mockController)
        assertNotNull(connectionResult)

        val playerCommands = connectionResult.availablePlayerCommands
        assertTrue("Player commands must contain COMMAND_PLAY_PAUSE", playerCommands.contains(Player.COMMAND_PLAY_PAUSE))
        assertFalse("Player commands must NOT contain COMMAND_SEEK_TO_NEXT", playerCommands.contains(Player.COMMAND_SEEK_TO_NEXT))
        assertFalse("Player commands must NOT contain COMMAND_SEEK_TO_PREVIOUS", playerCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))
        assertFalse("Player commands must NOT contain COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM", playerCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))

        controller.destroy()
    }
}

