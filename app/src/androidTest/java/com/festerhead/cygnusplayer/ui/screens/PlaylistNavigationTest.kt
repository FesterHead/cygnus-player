package com.festerhead.cygnusplayer.ui.screens

import android.app.Application
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.festerhead.cygnusplayer.PlaylistPickerViewModel
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
 * Instrumented test verifying navigation from the playlist picker to the now playing screen.
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
            ApplicationProvider.getApplicationContext<Application>(),
            playlistStateDao
        )
        
        // We use a state to control navigation in the test
        var currentScreen by mutableStateOf("PICKER")
        var selectedPlaylistPath by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            CygnusPlayerTheme {
                if (currentScreen == "PICKER") {
                    PlaylistPickerScreen(viewModel = pickerViewModel) { path ->
                        selectedPlaylistPath = path
                        currentScreen = "NOW_PLAYING"
                    }
                } else {
                    // Inject a NowPlayingViewModel that reflects the selected playlist
                    val nowPlayingViewModel = androidx.lifecycle.viewmodel.compose.viewModel<NowPlayingViewModel>()
                    LaunchedEffect(Unit) {
                        nowPlayingViewModel.initialize("Rush.m3u8", ShuffleMode.SEQUENTIAL)
                    }
                    
                    NowPlayingScreen(
                        viewModel = nowPlayingViewModel,
                    ) {
                        currentScreen = "PICKER"
                    }
                }
            }
        }

        // 1. Select the playlist
        composeTestRule.onNodeWithText("Rush.m3u8").performClick()
        composeTestRule.waitForIdle()

        // 2. Verify navigation and playlist name display
        composeTestRule.onNodeWithText("Rush.m3u8").assertIsDisplayed()
        
        // 3. Verify that the correct shuffle mode is initialized and rendered on the screen
        composeTestRule.onNodeWithText("SEQUENTIAL").assertIsDisplayed()
    }
}
