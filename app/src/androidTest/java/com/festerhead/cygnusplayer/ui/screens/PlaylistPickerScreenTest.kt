package com.festerhead.cygnusplayer.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.activity.ComponentActivity
import com.festerhead.cygnusplayer.PlaylistPickerViewModel
import com.festerhead.cygnusplayer.data.daos.PlaylistStateDao
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for [PlaylistPickerScreen].
 */
@RunWith(AndroidJUnit4::class)
class PlaylistPickerScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val playlistStateDao = mockk<PlaylistStateDao>(relaxed = true)
    private lateinit var viewModel: PlaylistPickerViewModel

    @Before
    fun setUp() {
        // Mock the initial history load
        coEvery { playlistStateDao.getAllStates() } returns listOf(
            PlaylistStateEntity("/storage/music/Rush.m3u8", 0, ShuffleMode.SEQUENTIAL, 1000L),
            PlaylistStateEntity("/storage/music/Hemispheres.m3u", 2112, ShuffleMode.TRACK_RANDOM, 2000L),
        )
        
        viewModel = PlaylistPickerViewModel(
            ApplicationProvider.getApplicationContext(),
            playlistStateDao
        )
    }

    @Test
    fun testBrandingAndGuidance() {
        composeTestRule.setContent {
            CygnusPlayerTheme {
                PlaylistPickerScreen(
                    viewModel = viewModel,
                    onPlaylistSelected = {}
                ) {}
            }
        }

        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Cygnus Player")).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify branding title
        composeTestRule.onNodeWithText("Cygnus Player").assertIsDisplayed()
        
        // Verify guidance text
        composeTestRule.onNodeWithText("M3U / M3U8 Playlists Only").assertIsDisplayed()
    }

    @Test
    fun testHistoryListRendering() {
        composeTestRule.setContent {
            CygnusPlayerTheme {
                PlaylistPickerScreen(
                    viewModel = viewModel,
                    onPlaylistSelected = {}
                ) {}
            }
        }

        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Rush.m3u8")).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify that filenames (from paths) are rendered
        composeTestRule.onNodeWithText("Rush.m3u8").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hemispheres.m3u").assertIsDisplayed()

        // Verify shuffle modes are rendered with friendly labels (now inside Boxes)
        composeTestRule.onNodeWithText("SEQUENTIAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("CHAOS (RANDOM)").assertIsDisplayed()
    }

    @Test
    fun testErrorSnackbarVisibility() {
        composeTestRule.setContent {
            CygnusPlayerTheme {
                PlaylistPickerScreen(
                    viewModel = viewModel,
                    onPlaylistSelected = {}
                ) {}
            }
        }

        // Manually trigger an error state in the ViewModel
        viewModel.onPlaylistSelected(android.net.Uri.parse("file:///invalid.txt"))

        // Verify the snackbar message appears
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("Unsupported format. M3U/M3U8 only.").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }
}
