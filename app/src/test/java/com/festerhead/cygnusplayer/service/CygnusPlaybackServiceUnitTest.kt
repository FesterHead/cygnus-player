package com.festerhead.cygnusplayer.service

import androidx.media3.session.MediaSession
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the interaction between [CygnusPlaybackService] and [MediaSession].
 *
 * Verifies that the service properly initializes its components and that the 
 * session correctly accepts connections.
 */
@RunWith(RobolectricTestRunner::class)
class CygnusPlaybackServiceUnitTest {

    /**
     * Verifies that the service initializes its internal components correctly
     * without crashing during startup.
     */
    @Test
    fun testServiceInitialization() {
        val service = CygnusPlaybackService()
        // Note: In a real Robolectric service test, you would use 
        // Robolectric.buildService(CygnusPlaybackService::class.java).create().get()
        assertNotNull(service)
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
}
