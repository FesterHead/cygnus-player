package com.festerhead.cygnusplayer.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for [NowPlayingScreen].
 */
class NowPlayingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testScreenDisplaysCorrectInformation() {
        composeTestRule.setContent {
            CygnusPlayerTheme {
                // Using a simple ViewModel instance for UI verification of defaults
                NowPlayingScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("No track playing")).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("No track playing").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back to Playlists").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Song Details").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun testSongDetailsDialogOpensAndDisplaysMetadata() {
        val testDetails = com.festerhead.cygnusplayer.ui.model.SongDetails(
            artistName = "Rush",
            trackTitle = "Limelight",
            albumTitle = "Moving Pictures",
            date = "1981",
            genre = "Rock",
            comment = "40th Anniversary Deluxe Edition",
            trackGain = "-1.61 dB",
            trackPeak = "0.898102",
            albumGain = "-1.71 dB",
            albumPeak = "1.000000",
        )

        composeTestRule.setContent {
            CygnusPlayerTheme {
                NowPlayingContent(
                    uiState = com.festerhead.cygnusplayer.ui.viewmodel.NowPlayingUiState(
                        trackTitle = "Limelight",
                        albumName = "Moving Pictures",
                        songDetails = testDetails,
                    ),
                    onNavigateBack = {},
                    onTogglePlayPause = {},
                )
            }
        }

        // Click Song Details button
        composeTestRule.onNodeWithContentDescription("Song Details").performClick()

        // Verify Dialog Title and Table Content
        composeTestRule.onNodeWithText("Song Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("NAME").assertIsDisplayed()
        composeTestRule.onNodeWithText("VALUE").assertIsDisplayed()

        composeTestRule.onNodeWithText("Artist Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rush").assertIsDisplayed()
        composeTestRule.onNodeWithText("Track Gain").assertIsDisplayed()
        composeTestRule.onNodeWithText("-1.61 dB").assertIsDisplayed()
        composeTestRule.onNodeWithText("Track Peak").assertIsDisplayed()
        composeTestRule.onNodeWithText("0.898102").assertIsDisplayed()

        // Dismiss dialog
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.onNodeWithText("Song Details").assertDoesNotExist()
    }
}
