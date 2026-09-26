package com.festerhead.cygnusplayer.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.festerhead.cygnusplayer.ui.model.SongDetails
import com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI verification tests for [SongDetailsDialog].
 */
class SongDetailsDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSongDetailsDialogDisplaysPopulatedFields() {
        var dismissed = false
        val details = SongDetails(
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
                SongDetailsDialog(
                    songDetails = details,
                    onDismissRequest = { dismissed = true },
                )
            }
        }

        // Title and table header
        composeTestRule.onNodeWithText("Song Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("NAME").assertIsDisplayed()
        composeTestRule.onNodeWithText("VALUE").assertIsDisplayed()

        // Rows
        composeTestRule.onNodeWithText("Artist Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rush").assertIsDisplayed()

        composeTestRule.onNodeWithText("Track Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Limelight").assertIsDisplayed()

        composeTestRule.onNodeWithText("Album Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Moving Pictures").assertIsDisplayed()

        composeTestRule.onNodeWithText("Date").assertIsDisplayed()
        composeTestRule.onNodeWithText("1981").assertIsDisplayed()

        composeTestRule.onNodeWithText("Genre").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rock").assertIsDisplayed()

        composeTestRule.onNodeWithText("Comment").assertIsDisplayed()
        composeTestRule.onNodeWithText("40th Anniversary Deluxe Edition").assertIsDisplayed()

        composeTestRule.onNodeWithText("Track Gain").assertIsDisplayed()
        composeTestRule.onNodeWithText("-1.61 dB").assertIsDisplayed()

        composeTestRule.onNodeWithText("Track Peak").assertIsDisplayed()
        composeTestRule.onNodeWithText("0.898102").assertIsDisplayed()

        composeTestRule.onNodeWithText("Album Gain").assertIsDisplayed()
        composeTestRule.onNodeWithText("-1.71 dB").assertIsDisplayed()

        composeTestRule.onNodeWithText("Album Peak").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.000000").assertIsDisplayed()

        // Close button click triggers dismiss
        composeTestRule.onNodeWithText("Close").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun testSongDetailsDialogDisplaysEmptyFallbacks() {
        composeTestRule.setContent {
            CygnusPlayerTheme {
                SongDetailsDialog(
                    songDetails = SongDetails(),
                    onDismissRequest = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Song Details").assertIsDisplayed()
        val emptyNodes = composeTestRule.onAllNodesWithText("<empty>").fetchSemanticsNodes()
        assertTrue("Should have 10 <empty> fields", emptyNodes.size == 10)
    }
}
