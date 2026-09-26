package com.festerhead.cygnusplayer.core

import com.festerhead.cygnusplayer.data.daos.QueueDao
import com.festerhead.cygnusplayer.data.daos.TrackDao
import com.festerhead.cygnusplayer.data.entities.QueueEntity
import com.festerhead.cygnusplayer.data.entities.TrackEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [QueueController].
 */
class QueueControllerTest {

    private val queueDao = mockk<QueueDao>()
    private val trackDao = mockk<TrackDao>()
    private val controller = QueueController(queueDao, trackDao)

    @Test
    fun testInitializationAndWindowResolution() = runBlocking {
        val mapping = longArrayOf(1L, 2L, 3L, 4L, 5L)
        val startId = 3L
        
        controller.initialize(mapping, startId)
        
        assertEquals(startId, controller.getCurrentQueueId())
        assertEquals("3/5", controller.getPositionString())
        
        // Mock DAO responses
        mockTrack(2L)
        mockTrack(3L)
        mockTrack(4L)
        
        val window = controller.getWindowData()
        
        assertEquals(2L, window.prev?.queueId)
        assertEquals(3L, window.current?.queueId)
        assertEquals(4L, window.next?.queueId)
    }

    @Test
    fun testBoundaryConditions() = runBlocking {
        val mapping = longArrayOf(1L, 2L)
        
        mockTrack(1L)
        mockTrack(2L)

        // First track
        controller.initialize(mapping, 1L)
        var window = controller.getWindowData()
        assertNull(window.prev)
        assertEquals(1L, window.current?.queueId)
        assertEquals(2L, window.next?.queueId)
        
        // Last track
        controller.initialize(mapping, 2L)
        window = controller.getWindowData()
        assertEquals(1L, window.prev?.queueId)
        assertEquals(2L, window.current?.queueId)
        assertNull(window.next)
    }

    @Test
    fun testNavigation() = runBlocking {
        val mapping = longArrayOf(1L, 2L, 3L)
        controller.initialize(mapping, 1L)
        
        controller.moveNext()
        assertEquals(2L, controller.getCurrentQueueId())
        assertEquals("2/3", controller.getPositionString())
        
        controller.moveNext()
        assertEquals(3L, controller.getCurrentQueueId())
        
        controller.moveNext() // Should not go past end
        assertEquals(3L, controller.getCurrentQueueId())
        
        controller.movePrevious()
        assertEquals(2L, controller.getCurrentQueueId())
    }

    @Test
    fun testClearQueue() {
        val mapping = longArrayOf(1L, 2L, 3L)
        controller.initialize(mapping, 2L)
        assertEquals("2/3", controller.getPositionString())
        assertEquals(2L, controller.getCurrentQueueId())

        controller.clear()
        assertEquals("0/0", controller.getPositionString())
        assertNull(controller.getCurrentQueueId())
        assertNull(controller.getCurrentTrackData())
        assertNull(controller.getNextTrackData())
    }

    /**
     * Verifies that [QueueController.moveNext] synchronously shifts the cached window
     * so that the next track immediately becomes current without waiting for database queries.
     */
    @Test
    fun testCachedWindowAndMoveNextShift() = runBlocking {
        val mapping = longArrayOf(1L, 2L, 3L)
        controller.initialize(mapping, 1L)

        mockTrack(1L)
        mockTrack(2L)
        mockTrack(3L)

        // Initial resolution
        val window = controller.getWindowData()
        assertEquals(1L, window.current?.queueId)
        assertEquals(2L, window.next?.queueId)
        assertEquals(1L, controller.getCurrentTrackData()?.queueId)
        assertEquals(2L, controller.getNextTrackData()?.queueId)

        // Shift forward synchronously
        controller.moveNext()
        assertEquals(2L, controller.getCurrentTrackData()?.queueId)
        assertEquals(2L, controller.getCurrentTrackData()?.track?.trackId)
        assertNull(controller.getNextTrackData())

        // Calling getWindowData() resolves the new next (3L) while preserving current (2L)
        val newWindow = controller.getWindowData()
        assertEquals(1L, newWindow.prev?.queueId)
        assertEquals(2L, newWindow.current?.queueId)
        assertEquals(3L, newWindow.next?.queueId)
        assertEquals(2L, controller.getCurrentTrackData()?.queueId)
        assertEquals(3L, controller.getNextTrackData()?.queueId)
    }

    /**
     * Verifies that [QueueController.updateTrackInCache] updates the in-memory track data,
     * allowing newly extracted ReplayGain tags to be immediately accessible.
     */
    @Test
    fun testUpdateTrackInCache() = runBlocking {
        val mapping = longArrayOf(1L, 2L)
        controller.initialize(mapping, 1L)

        mockTrack(1L)
        mockTrack(2L)

        controller.getWindowData()
        assertEquals(2L, controller.getNextTrackData()?.queueId)
        assertNull(controller.getNextTrackData()?.track?.trackGain)

        // Simulate background metadata extractor saving gain tags
        val updatedTrack = TrackEntity(
            trackId = 2L,
            filePath = "path/2",
            folderPath = "folder",
            trackGain = -5.45f,
            albumGain = -6.12f,
        )
        controller.updateTrackInCache(updatedTrack)

        // Verify cache updated immediately
        assertEquals(-5.45f, controller.getNextTrackData()?.track?.trackGain)
        assertEquals(-6.12f, controller.getNextTrackData()?.track?.albumGain)

        // After moveNext(), the updated track is synchronously the current track
        controller.moveNext()
        assertEquals(2L, controller.getCurrentTrackData()?.queueId)
        assertEquals(-5.45f, controller.getCurrentTrackData()?.track?.trackGain)
        assertEquals(-6.12f, controller.getCurrentTrackData()?.track?.albumGain)
    }

    private fun mockTrack(id: Long) {
        val queueEntry = QueueEntity(queueId = id, trackId = id, filePath = "path/$id", folderPath = "folder", m3uPath = "test.m3u8")
        val track = TrackEntity(trackId = id, filePath = "path/$id", folderPath = "folder")
        coEvery { queueDao.getQueueEntryById(id) } returns queueEntry
        coEvery { trackDao.getTrackById(id) } returns track
    }
}
