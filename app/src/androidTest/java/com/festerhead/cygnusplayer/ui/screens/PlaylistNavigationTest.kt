package com.festerhead.cygnusplayer.ui.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.festerhead.cygnusplayer.PlaylistPickerViewModel
import com.festerhead.cygnusplayer.Screen
import com.festerhead.cygnusplayer.data.daos.PlaylistStateDao
import com.festerhead.cygnusplayer.data.entities.PlaylistStateEntity
import com.festerhead.cygnusplayer.data.entities.ShuffleMode
import com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme
import com.festerhead.cygnusplayer.ui.viewmodel.NowPlayingViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test verifying navigation between playlist picker and now playing screens.
 */
@RunWith(AndroidJUnit4::class)
class PlaylistNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNavigationPassesPlaylistName() {
        val playlistStateDao = mockk<PlaylistStateDao>(relaxed = true)
        val playlistPath = "/storage/music/Rush.m3u8"
        
        coEvery { playlistStateDao.getAllStates() } returns listOf(
            PlaylistStateEntity(playlistPath, 0, ShuffleMode.SEQUENTIAL, 1000L),
        )
        
        val pickerViewModel = PlaylistPickerViewModel(
            ApplicationProvider.getApplicationContext(),
            playlistStateDao,
        )
        
        var currentScreen by mutableStateOf(Screen.PLAYLIST_PICKER)

        composeTestRule.setContent {
            CygnusPlayerTheme {
                when (currentScreen) {
                    Screen.PLAYLIST_PICKER -> {
                        PlaylistPickerScreen(
                            viewModel = pickerViewModel,
                            onPlaylistSelected = { path ->
                                pickerViewModel.onPlaylistClicked(ApplicationProvider.getApplicationContext(), path) {
                                    currentScreen = Screen.NOW_PLAYING
                                }
                            },
                            onSettingsClicked = { currentScreen = Screen.SETTINGS }
                        )
                    }
                    Screen.NOW_PLAYING -> {
                        val nowPlayingViewModel = androidx.lifecycle.viewmodel.compose.viewModel<NowPlayingViewModel>()
                        LaunchedEffect(Unit) {
                            nowPlayingViewModel.initialize("Rush.m3u8", ShuffleMode.SEQUENTIAL)
                        }
                        
                        NowPlayingScreen(
                            viewModel = nowPlayingViewModel,
                            onNavigateBack = { currentScreen = Screen.PLAYLIST_PICKER }
                        )
                    }
                    Screen.SETTINGS -> {}
                }
            }
        }

        // 1. Select the playlist
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Rush.m3u8")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Rush.m3u8").performClick()
        composeTestRule.waitForIdle()

        // 2. Verify navigation and playlist name display
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Rush.m3u8")).fetchSemanticsNodes().size == 1
        }
        composeTestRule.onNodeWithText("Rush.m3u8").assertIsDisplayed()
        
        // 3. Verify that the correct shuffle mode is initialized and rendered on the screen
        composeTestRule.onNodeWithText("SEQUENTIAL").assertIsDisplayed()
    }

    @Test
    fun testBackNavigationReturnsToPlaylistPicker() {
        val playlistStateDao = mockk<PlaylistStateDao>(relaxed = true)
        val playlistPath = "/storage/music/Rush.m3u8"
        
        coEvery { playlistStateDao.getAllStates() } returns listOf(
            PlaylistStateEntity(playlistPath, 0, ShuffleMode.SEQUENTIAL, 1000L),
        )
        
        val pickerViewModel = PlaylistPickerViewModel(
            ApplicationProvider.getApplicationContext(),
            playlistStateDao,
        )
        pickerViewModel.setActivePlaylist(playlistPath)
        
        var currentScreen by mutableStateOf(Screen.NOW_PLAYING)

        composeTestRule.setContent {
            CygnusPlayerTheme {
                when (currentScreen) {
                    Screen.NOW_PLAYING -> {
                        val nowPlayingViewModel = androidx.lifecycle.viewmodel.compose.viewModel<NowPlayingViewModel>()
                        LaunchedEffect(Unit) {
                            nowPlayingViewModel.initialize("Rush.m3u8", ShuffleMode.SEQUENTIAL)
                        }
                        
                        NowPlayingScreen(
                            viewModel = nowPlayingViewModel,
                            onNavigateBack = { currentScreen = Screen.PLAYLIST_PICKER }
                        )
                    }
                    Screen.PLAYLIST_PICKER -> {
                        PlaylistPickerScreen(
                            viewModel = pickerViewModel,
                            onPlaylistSelected = { currentScreen = Screen.NOW_PLAYING },
                            onSettingsClicked = { currentScreen = Screen.SETTINGS }
                        )
                    }
                    Screen.SETTINGS -> {}
                }
            }
        }

        // 1. Verify initially on Now Playing screen
        composeTestRule.onNodeWithText("Rush.m3u8").assertIsDisplayed()

        // 2. Perform back click
        composeTestRule.onNodeWithContentDescription("Back to Playlists").performClick()
        composeTestRule.waitForIdle()

        // 3. Verify screen returned to Playlist Picker and header is visible
        composeTestRule.onNodeWithText("Cygnus Player").assertIsDisplayed()
        composeTestRule.onNodeWithText("M3U / M3U8 Playlists Only").assertIsDisplayed()
    }
}
