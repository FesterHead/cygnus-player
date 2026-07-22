package com.festerhead.cygnusplayer.data.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.festerhead.cygnusplayer.data.CygnusDatabase
import com.festerhead.cygnusplayer.data.entities.TrackEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for [TrackDao].
 * Verifies metadata persistence and unique constraints using realistic Android paths.
 */
@RunWith(AndroidJUnit4::class)
class TrackDaoTest {
    private lateinit var trackDao: TrackDao
    private lateinit var db: CygnusDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, CygnusDatabase::class.java
        ).build()
        trackDao = db.trackDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    /**
     * Verifies that a track can be inserted and retrieved by both its path and its auto-generated ID.
     */
    @Test
    @Throws(Exception::class)
    fun writeTrackAndReadInList() {
        runBlocking {
            val path = "/storage/emulated/0/Music/Rush/Hemispheres/01 Cygnus X-1 Book II.mp3"
            val track = TrackEntity(
                filePath = path,
                folderPath = "/storage/emulated/0/Music/Rush/Hemispheres",
                title = "Cygnus X-1 Book II: Hemispheres",
                artist = "Rush",
                album = "Hemispheres",
                trackGain = -0.5f,
                albumGain = -1.2f
            )
            val id = trackDao.insert(track)
            
            val byPath = trackDao.getTrackByPath(path)
            assertEquals("Cygnus X-1 Book II: Hemispheres", byPath?.title)
            
            val byId = trackDao.getTrackById(id)
            assertEquals(path, byId?.filePath)
        }
    }

    /**
     * Verifies that the database correctly persists and indexes realistic Android file paths.
     */
    @Test
    fun testAndroidStylePathPersistence() {
        runBlocking {
            val path = "/storage/emulated/0/Music/Rush/Moving Pictures/01 Tom Sawyer.mp3"
            val track = TrackEntity(
                filePath = path,
                folderPath = "/storage/emulated/0/Music/Rush/Moving Pictures",
                title = "Tom Sawyer",
                artist = "Rush",
                album = "Moving Pictures",
                trackGain = 0f,
                albumGain = 0f
            )
            trackDao.insert(track)
            val result = trackDao.getTrackByPath(path)
            assertEquals("Tom Sawyer", result?.title)
            assertEquals(path, result?.filePath)
        }
    }

    /**
     * Ensures that the database enforces the unique constraint on the file_path column.
     * Attempting to insert a duplicate path must throw an [android.database.sqlite.SQLiteConstraintException].
     */
    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun testDuplicateFilePathConstraint() {
        runBlocking {
            val path = "/storage/emulated/0/Music/Rush/Permanent Waves/01 The Spirit of Radio.mp3"
            val track1 = TrackEntity(filePath = path, folderPath = "/storage/emulated/0/Music/Rush/Permanent Waves", title = "The Spirit of Radio", artist = "Rush", album = "Permanent Waves", trackGain = 0f, albumGain = 0f)
            val track2 = TrackEntity(filePath = path, folderPath = "/storage/emulated/0/Music/Rush/Permanent Waves", title = "The Spirit of Radio (Duplicate)", artist = "Rush", album = "Permanent Waves", trackGain = 0f, albumGain = 0f)

            trackDao.insert(track1)
            trackDao.insert(track2)
        }
    }

    /**
     * Verifies that the [TrackDao.deleteAll] function successfully clears all records from the table.
     */
    @Test
    fun testDeleteAllTracks() {
        runBlocking {
            val path = "/storage/emulated/0/Music/Rush/A Farewell to Kings/01 A Farewell to Kings.mp3"
            val track = TrackEntity(filePath = path, folderPath = "/storage/emulated/0/Music/Rush/A Farewell to Kings", title = "A Farewell to Kings", artist = "Rush", album = "A Farewell to Kings", trackGain = 0f, albumGain = 0f)
            trackDao.insert(track)
            trackDao.deleteAll()
            val result = trackDao.getTrackByPath(path)
            assertNull(result)
        }
    }

    /**
     * Verifies that multiple tracks can be inserted efficiently using insertAll.
     */
    @Test
    fun testBatchInsertTracks() {
        runBlocking {
            val path1 = "/storage/emulated/0/Music/Rush/Fly By Night/01 Anthem.mp3"
            val path2 = "/storage/emulated/0/Music/Rush/Caress of Steel/01 Bastille Day.mp3"
            
            val tracks = listOf(
                TrackEntity(filePath = path1, folderPath = "/storage/emulated/0/Music/Rush/Fly By Night", title = "Anthem", artist = "Rush", album = "Fly By Night", trackGain = 0f, albumGain = 0f),
                TrackEntity(filePath = path2, folderPath = "/storage/emulated/0/Music/Rush/Caress of Steel", title = "Bastille Day", artist = "Rush", album = "Caress of Steel", trackGain = 0f, albumGain = 0f)
            )
            trackDao.insertAll(tracks)
            
            val t1 = trackDao.getTrackByPath(path1)
            val t2 = trackDao.getTrackByPath(path2)
            
            assertEquals("Anthem", t1?.title)
            assertEquals("Bastille Day", t2?.title)
            
            trackDao.deleteAll()
            assertNull(trackDao.getTrackByPath(path1))
        }
    }

    /**
     * Verifies that tracks with missing metadata correctly use the default "<not found>" string.
     */
    @Test
    fun testTrackMetadataDefaults() {
        runBlocking {
            val path = "/storage/emulated/0/Music/Rush/Unknown/01 Unknown.mp3"
            val track = TrackEntity(
                filePath = path,
                folderPath = "/storage/emulated/0/Music/Rush/Unknown"
            )
            trackDao.insert(track)
            
            val result = trackDao.getTrackByPath(path)
            assertEquals("<not found>", result?.title)
            assertEquals("<not found>", result?.artist)
            assertEquals("<not found>", result?.album)
        }
    }
}
