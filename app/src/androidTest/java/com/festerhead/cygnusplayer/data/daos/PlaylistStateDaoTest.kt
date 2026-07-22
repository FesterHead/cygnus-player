package com.festerhead.cygnusplayer.data.daos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.festerhead.cygnusplayer.data.CygnusDatabase
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for [PlaylistStateDao].
 * Verifies persistence of playback position and shuffle modes per M3U file.
 */
@RunWith(AndroidJUnit4::class)
class PlaylistStateDaoTest {
    private lateinit var playlistStateDao: PlaylistStateDao
    private lateinit var db: CygnusDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, CygnusDatabase::class.java
        ).build()
        playlistStateDao = db.playlistStateDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    /**
     * Verifies that state can be saved and retrieved for a Unix-style M3U path.
     */
    @Test
    fun testUnixPlaylistStatePersistence() {
        runBlocking {
            val m3uPath = "/storage/emulated/0/Playlists/Rush_Epic_Suites.m3u8"
            val state = PlaylistStateEntity(
                m3uPath = m3uPath,
                lastQueueId = 2112,
                shuffleMode = ShuffleMode.RANDOM_FOLDER_SEQUENTIAL
            )
            playlistStateDao.saveState(state)
            
            val result = playlistStateDao.getStateForPlaylist(m3uPath)
            assertEquals(2112L, result?.lastQueueId)
            assertEquals(ShuffleMode.RANDOM_FOLDER_SEQUENTIAL, result?.shuffleMode)
        }
    }

    /**
     * Verifies that state can be saved and retrieved for a Windows-style M3U path.
     */
    @Test
    fun testWindowsPlaylistStatePersistence() {
        runBlocking {
            val m3uPath = "C:\\Users\\Steve\\Music\\Playlists\\Rush_Discovery.m3u"
            val state = PlaylistStateEntity(
                m3uPath = m3uPath,
                lastQueueId = 1974,
                shuffleMode = ShuffleMode.SEQUENTIAL
            )
            playlistStateDao.saveState(state)
            
            val result = playlistStateDao.getStateForPlaylist(m3uPath)
            assertEquals(1974L, result?.lastQueueId)
            assertEquals(ShuffleMode.SEQUENTIAL, result?.shuffleMode)
        }
    }

    /**
     * Verifies that saving state for an existing path performs an update (REPLACE).
     */
    @Test
    fun testUpdatePlaylistState() {
        runBlocking {
            val m3uPath = "/storage/emulated/0/Playlists/Rush_Permanent_Waves.m3u"
            val initialState = PlaylistStateEntity(m3uPath, 1980, ShuffleMode.SEQUENTIAL)
            val updatedState = PlaylistStateEntity(m3uPath, 1981, ShuffleMode.TRACK_RANDOM)
            
            playlistStateDao.saveState(initialState)
            playlistStateDao.saveState(updatedState)
            
            val result = playlistStateDao.getStateForPlaylist(m3uPath)
            assertEquals(1981L, result?.lastQueueId)
            assertEquals(ShuffleMode.TRACK_RANDOM, result?.shuffleMode)
        }
    }

    /**
     * Verifies that a specific playlist state can be deleted.
     */
    @Test
    fun testDeletePlaylistState() {
        runBlocking {
            val m3uPath = "/storage/emulated/0/Playlists/Temporary_Rush_Live_Bootleg.m3u8"
            val state = PlaylistStateEntity(m3uPath, 1976, ShuffleMode.SEQUENTIAL)
            playlistStateDao.saveState(state)
            
            playlistStateDao.deleteState(m3uPath)
            
            val result = playlistStateDao.getStateForPlaylist(m3uPath)
            assertNull(result)
        }
    }

    /**
     * Verifies that the database can manage multiple playlist states with mixed path styles.
     */
    @Test
    fun testCrossPlatformPathHandlingInPlaylistStates() {
        runBlocking {
            val winPath = "E:\\Music\\Playlists\\Rush_Studio_Albums.m3u"
            val unixPath = "/storage/emulated/0/Playlists/Rush_Live_Archives.m3u8"
            
            val s1 = PlaylistStateEntity(winPath, 1974, ShuffleMode.SEQUENTIAL)
            val s2 = PlaylistStateEntity(unixPath, 1989, ShuffleMode.TRACK_RANDOM)
            
            playlistStateDao.saveState(s1)
            playlistStateDao.saveState(s2)
            
            val r1 = playlistStateDao.getStateForPlaylist(winPath)
            val r2 = playlistStateDao.getStateForPlaylist(unixPath)
            
            assertEquals(1974L, r1?.lastQueueId)
            assertEquals(1989L, r2?.lastQueueId)
            
            val all = playlistStateDao.getAllStates()
            assertEquals(2, all.size)
            
            playlistStateDao.deleteState(winPath)
            assertNull(playlistStateDao.getStateForPlaylist(winPath))
        }
    }
}
