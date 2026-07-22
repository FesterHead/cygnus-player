package com.festerhead.cygnusplayer.data.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.festerhead.cygnusplayer.data.CygnusDatabase
import com.festerhead.cygnusplayer.data.entities.QueueEntity
import com.festerhead.cygnusplayer.data.entities.TrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for [QueueDao].
 * Verifies sequence integrity and support for duplicate track entries using 
 * realistic Android paths.
 */
@RunWith(AndroidJUnit4::class)
class QueueDaoTest {
    private lateinit var queueDao: QueueDao
    private lateinit var trackDao: TrackDao
    private lateinit var db: CygnusDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, CygnusDatabase::class.java
        ).build()
        queueDao = db.queueDao()
        trackDao = db.trackDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    /**
     * Verifies that multiple occurrences of the same track can exist in the queue
     * and each is assigned a unique, sequential queue_id.
     */
    @Test
    fun testDuplicateTrackEntriesHaveUniqueIds() {
        runBlocking {
            val path = "/storage/emulated/0/Music/Rush/2112/01 Overture.mp3"
            val folder = "/storage/emulated/0/Music/Rush/2112"
            val m3u = "/storage/emulated/0/Music/FestersFavs/Rush.m3u8"
            val track = TrackEntity(
                filePath = path,
                folderPath = folder,
                title = "2112: Overture",
                artist = "Rush",
                album = "2112",
                trackGain = 0f,
                albumGain = 0f
            )
            val trackId = trackDao.insert(track)

            // Insert the same track 3 times (e.g., a "Best of" or repeated track in M3U)
            val entry1 = QueueEntity(trackId = trackId, filePath = track.filePath, folderPath = track.folderPath, m3uPath = m3u)
            val entry2 = QueueEntity(trackId = trackId, filePath = track.filePath, folderPath = track.folderPath, m3uPath = m3u)
            val entry3 = QueueEntity(trackId = trackId, filePath = track.filePath, folderPath = track.folderPath, m3uPath = m3u)

            val id1 = queueDao.insert(entry1)
            val id2 = queueDao.insert(entry2)
            val id3 = queueDao.insert(entry3)

            // Verify unique IDs are assigned
            assertNotEquals(id1, id2)
            assertNotEquals(id2, id3)
            
            // Verify sequential ordering
            assertTrue(id2 > id1)
            assertTrue(id3 > id2)

            val queue = queueDao.getAllQueueEntries()
            assertEquals(3, queue.size)
            assertEquals(trackId, queue[0].trackId)
            assertEquals(trackId, queue[2].trackId)
        }
    }

    /**
     * Verifies that unique folders can be extracted from the queue for shuffle logic.
     */
    @Test
    fun testGetAllUniqueFolders() {
        runBlocking {
            val f1 = "/storage/emulated/0/Music/Rush/Signals"
            val f2 = "/storage/emulated/0/Music/Rush/Grace Under Pressure"
            val m3u = "/storage/emulated/0/Music/FestersFavs/Rush.m3u8"
            val track1 = TrackEntity(
                filePath = "$f1/01 Subdivisions.mp3", 
                folderPath = f1, 
                title = "Subdivisions", 
                artist = "Rush", 
                album = "Signals", 
                trackGain = 0f, 
                albumGain = 0f
            )
            val track2 = TrackEntity(
                filePath = "$f2/01 Distant Early Warning.mp3", 
                folderPath = f2, 
                title = "Distant Early Warning", 
                artist = "Rush", 
                album = "Grace Under Pressure", 
                trackGain = 0f, 
                albumGain = 0f
            )
            val t1Id = trackDao.insert(track1)
            val t2Id = trackDao.insert(track2)

            queueDao.insertAll(listOf(
                QueueEntity(trackId = t1Id, filePath = track1.filePath, folderPath = track1.folderPath, m3uPath = m3u),
                QueueEntity(trackId = t1Id, filePath = track1.filePath, folderPath = track1.folderPath, m3uPath = m3u),
                QueueEntity(trackId = t2Id, filePath = track2.filePath, folderPath = track2.folderPath, m3uPath = m3u)
            ))

            val folders = queueDao.getAllUniqueFoldersForPlaylist(m3u)
            assertEquals(2, folders.size)
            assertTrue(folders.contains(f1))
            assertTrue(folders.contains(f2))
        }
    }

    /**
     * Verifies that tracks can be filtered by folder in sequential order.
     */
    @Test
    fun testGetTracksByFolder() {
        runBlocking {
            val f1 = "/storage/emulated/0/Music/Rush/Power Windows"
            val f2 = "/storage/emulated/0/Music/Rush/Hold Your Fire"
            val m3u = "/storage/emulated/0/Music/FestersFavs/Rush.m3u8"
            val t1 = TrackEntity(
                filePath = "$f1/01 The Big Money.mp3", 
                folderPath = f1, 
                title = "The Big Money", 
                artist = "Rush", 
                album = "Power Windows", 
                trackGain = 0f, 
                albumGain = 0f
            )
            val t2 = TrackEntity(
                filePath = "$f2/01 Force Ten.mp3", 
                folderPath = f2, 
                title = "Force Ten", 
                artist = "Rush", 
                album = "Hold Your Fire", 
                trackGain = 0f, 
                albumGain = 0f
            )
            val t1Id = trackDao.insert(t1)
            val t2Id = trackDao.insert(t2)

            queueDao.insertAll(listOf(
                QueueEntity(trackId = t1Id, filePath = t1.filePath, folderPath = t1.folderPath, m3uPath = m3u),
                QueueEntity(trackId = t2Id, filePath = t2.filePath, folderPath = t2.folderPath, m3uPath = m3u),
                QueueEntity(trackId = t1Id, filePath = t1.filePath, folderPath = t1.folderPath, m3uPath = m3u)
            ))

            val f1Tracks = queueDao.getTracksByFolderForPlaylist(f1, m3u)
            assertEquals(2, f1Tracks.size)
            assertEquals("$f1/01 The Big Money.mp3", f1Tracks[0].filePath)
            assertEquals("$f1/01 The Big Money.mp3", f1Tracks[1].filePath)
            assertTrue(f1Tracks[1].queueId > f1Tracks[0].queueId)
        }
    }

    /**
     * Verifies the full lifecycle of the queue including clear operations.
     */
    @Test
    fun testQueueLifecycle() {
        runBlocking {
            val path = "/storage/emulated/0/Music/Rush/Counterparts/01 Animate.mp3"
            val folder = "/storage/emulated/0/Music/Rush/Counterparts"
            val m3u = "/storage/emulated/0/Music/FestersFavs/Rush.m3u8"
            val track = TrackEntity(
                filePath = path,
                folderPath = folder,
                title = "Animate",
                artist = "Rush",
                album = "Counterparts",
                trackGain = 0f,
                albumGain = 0f
            )
            val trackId = trackDao.insert(track)
            queueDao.insert(QueueEntity(trackId = trackId, filePath = track.filePath, folderPath = track.folderPath, m3uPath = m3u))
            
            assertEquals(1, queueDao.getAllQueueEntries().size)
            
            queueDao.clearAll()
            
            assertEquals(0, queueDao.getAllQueueEntries().size)
        }
    }

    /**
     * Verifies that the queue can filter and isolate entries by m3u_path.
     */
    @Test
    fun testM3uPathIsolation() {
        runBlocking {
            val m3u1 = "/playlists/test1.m3u8"
            val m3u2 = "/playlists/test2.m3u8"
            
            val track = TrackEntity(filePath = "/track.mp3", folderPath = "/", title = "T", artist = "A", album = "A")
            val trackId = trackDao.insert(track)
            
            queueDao.insert(QueueEntity(trackId = trackId, filePath = track.filePath, folderPath = track.folderPath, m3uPath = m3u1))
            queueDao.insert(QueueEntity(trackId = trackId, filePath = track.filePath, folderPath = track.folderPath, m3uPath = m3u2))
            
            assertEquals(1, queueDao.getQueueIdsForPlaylist(m3u1).size)
            assertEquals(1, queueDao.getQueueIdsForPlaylist(m3u2).size)
            
            queueDao.clearQueueForPlaylist(m3u1)
            assertEquals(0, queueDao.getQueueIdsForPlaylist(m3u1).size)
            assertEquals(1, queueDao.getQueueIdsForPlaylist(m3u2).size)
        }
    }
}
