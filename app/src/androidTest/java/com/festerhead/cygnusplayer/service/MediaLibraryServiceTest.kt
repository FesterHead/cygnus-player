package com.festerhead.cygnusplayer.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.festerhead.cygnusplayer.CygnusApplication
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

/**
 * Instrumented tests for [CygnusPlaybackService]'s Android Auto (MediaLibrary) integration.
 * Verifies that the library root and history-based children are correctly exposed to car head units.
 */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class MediaLibraryServiceTest {

    private lateinit var context: Context
    private lateinit var browser: MediaBrowser

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        val sessionToken = SessionToken(context, ComponentName(context, CygnusPlaybackService::class.java))
        
        // Build the MediaBrowser on the Main thread to ensure compliance with Media3 threading
        withContext(Dispatchers.Main) {
            val browserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
            browser = browserFuture.await()
            // Give the session a moment to settle
            kotlinx.coroutines.delay(1000.milliseconds)
        }
    }

    @After
    fun tearDown() = runBlocking {
        withContext(Dispatchers.Main) {
            browser.release()
        }
    }

    @Test
    fun testGetLibraryRoot() = runBlocking {
        // Ensure we are on the Main thread for MediaBrowser calls
        val rootResult = withContext(Dispatchers.Main) {
            val future = browser.getLibraryRoot(null)
            future.await()
        }
        
        val rootItem = rootResult.value
        assertTrue("Root result should be success", rootResult.resultCode == LibraryResult.RESULT_SUCCESS)
        assertEquals("RECENT_ROOT", rootItem?.mediaId)
        assertEquals("Recent Playlists", rootItem?.mediaMetadata?.title)
    }

    @Test
    fun testGetChildrenReturnsPlaylistHistory() = runBlocking {
        val app = context.applicationContext as CygnusApplication
        val dao = app.database.playlistStateDao()
        
        // 1. Setup mock history in the database
        val mockPlaylist1 = PlaylistStateEntity(
            m3uPath = "/music/rush.m3u8",
            lastQueueId = 1L,
            shuffleMode = ShuffleMode.SEQUENTIAL,
            lastOpened = System.currentTimeMillis(),
        )
        val mockPlaylist2 = PlaylistStateEntity(
            m3uPath = "/music/yes.m3u8",
            lastQueueId = 2L,
            shuffleMode = ShuffleMode.SEQUENTIAL,
            lastOpened = System.currentTimeMillis() - 1000,
        )
        
        dao.saveState(mockPlaylist1)
        dao.saveState(mockPlaylist2)

        // 2. Fetch children for the "RECENT_ROOT" node
        val childrenResult = withContext(Dispatchers.Main) {
            browser.getChildren("RECENT_ROOT", 0, 100, null).await()
        }
        val children = childrenResult.value ?: emptyList()

        // 3. Verify mapping
        assertTrue("Should have at least 2 children", children.size >= 2)
        
        val titles = children.map { it.mediaMetadata.title.toString() }
        assertTrue("Rush playlist should be in the list", titles.contains("rush.m3u8"))
        assertTrue("Yes playlist should be in the list", titles.contains("yes.m3u8"))
        
        // Verify mediaId format (PLAYLIST|path)
        val firstChild = children.first()
        assertTrue("MediaId should start with PLAYLIST|", firstChild.mediaId.startsWith("PLAYLIST|"))
    }

    @Test
    fun testPlayPauseCommandRouting() = runBlocking {
        withContext(Dispatchers.Main) {
            // 1. Trigger Play
            browser.play()
        }
        
        // Wait a bit for the command to propagate
        kotlinx.coroutines.delay(500.milliseconds)
        
        withContext(Dispatchers.Main) {
            // 2. Trigger Pause
            browser.pause()
        }
        kotlinx.coroutines.delay(500.milliseconds)
        
        withContext(Dispatchers.Main) {
            assertTrue("Should be paused after pause command", !browser.isPlaying)
        }
    }
}
