package com.festerhead.cygnusplayer.data

import android.content.Context
import android.net.Uri
import com.festerhead.cygnusplayer.core.ShuffleEngine
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.data.metadata.Media3MetadataExtractor
import com.festerhead.cygnusplayer.data.parser.M3uEntry
import com.festerhead.cygnusplayer.data.parser.M3uParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStream

/**
 * Unit tests for [PlaylistRepository].
 * Verifies the orchestration of parsing, metadata extraction, and sequence generation.
 */
@RunWith(RobolectricTestRunner::class)
class PlaylistRepositoryTest {

    private lateinit var context: Context
    private val m3uParser = mockk<M3uParser>()
    private val metadataExtractor = mockk<Media3MetadataExtractor>()
    private val shuffleEngine = mockk<ShuffleEngine>()
    private lateinit var database: CygnusDatabase

    private lateinit var repository: PlaylistRepository

    @Before
    fun setUp() {
        context = androidx.test.core.app.ApplicationProvider.getApplicationContext()

        database = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            CygnusDatabase::class.java,
        ).allowMainThreadQueries().build()

        repository = PlaylistRepository(
            context,
            database,
            m3uParser,
            metadataExtractor,
            database.trackDao(),
            database.queueDao(),
            database.playlistStateDao(),
            shuffleEngine,
        )

        mockkStatic(Uri::class)
        every { Uri.fromFile(any()) } returns mockk(relaxed = true)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
        every { Uri.decode(any()) } answers { firstArg() }

        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
    }

    @org.junit.After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `loadPlaylist parses M3U and generates mapping if missing`() = runBlocking {
        val path = "/music/rush.m3u8"
        val state = PlaylistStateEntity(path, 0, ShuffleMode.SEQUENTIAL)
        
        database.playlistStateDao().saveState(state)

        coEvery { m3uParser.parse(any<InputStream>()) } returns sequenceOf(
            M3uEntry("track1.mp3"),
            M3uEntry("track2.mp3"),
        )
        
        coEvery { shuffleEngine.generateMapping(any(), any(), any(), any()) } returns longArrayOf(1L, 2L)

        val result = repository.loadPlaylist(path)

        assertEquals(2, result?.mapping?.size)
        
        coVerify { m3uParser.parse(any<InputStream>()) }
    }

    @Test
    fun `loadPlaylist returns existing state if mapping exists`() = runBlocking {
        val path = "/music/rush.m3u8"
        val existingMapping = longArrayOf(1L, 2L)
        val state = PlaylistStateEntity(path, 1L, ShuffleMode.SEQUENTIAL, mapping = existingMapping)
        
        database.playlistStateDao().saveState(state)

        val result = repository.loadPlaylist(path)

        assertEquals(existingMapping.toList(), result?.mapping?.toList())
        coVerify(exactly = 0) { m3uParser.parse(any<InputStream>()) }
    }
}
