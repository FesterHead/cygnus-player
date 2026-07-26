package com.festerhead.cygnusplayer.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
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
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }
}
