package com.festerhead.cygnusplayer.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.festerhead.cygnusplayer.ui.theme.CygnusPlayerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsScreenRendersCorrectly() {
        composeTestRule.setContent {
            CygnusPlayerTheme {
                SettingsScreen(onNavigateBack = {})
            }
        }

        // Verify sections
        composeTestRule.onNodeWithText("CONFIGURATION").assertIsDisplayed()
        composeTestRule.onNodeWithText("DIAGNOSTICS").assertIsDisplayed()
        composeTestRule.onNodeWithText("ABOUT").assertIsDisplayed()

        // Verify specific items
        composeTestRule.onNodeWithText("Reset Music Root Folder").assertIsDisplayed()
        composeTestRule.onNodeWithText("GitHub Repository").assertIsDisplayed()
        composeTestRule.onNodeWithText("MIT License").assertIsDisplayed()
    }
}
