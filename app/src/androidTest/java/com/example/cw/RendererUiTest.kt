package com.example.cw

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertCountEquals
import org.junit.Rule
import org.junit.Test
import com.example.cw.ui.theme.CWTheme
import com.example.cw.prompt.MockedPromptProcessor

class SimpleUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockedJson = """
        [
          {"prompt":"Make background blue","uiDescription":{"components":[{"type":"background","color":"blue"}]}},
          {"prompt":"Add a profile card","uiDescription":{"components":[{"type":"card","properties":{"backgroundColor":"white","title":"Profile"}}]}},
          {"prompt":"Show title My Profile","uiDescription":{"components":[{"type":"title","text":"My Profile"}]}},
          {"prompt":"Add Save button","uiDescription":{"components":[{"type":"button","text":"Save","action":"saveProfile"}]}},
          {"prompt":"Reset","uiDescription":{"components":[]}}
        ]
    """.trimIndent()

    @Test
    fun simplified_ui_elements_are_present() {
        composeTestRule.setContent {
            CWTheme {
                SimpleScreen(processor = MockedPromptProcessor.fromJson(mockedJson))
            }
        }
        // Title centered (we just check text presence)
        composeTestRule.onNodeWithText("My Profile").assertIsDisplayed()
        // Name label and input
        composeTestRule.onNodeWithText("Name").assertIsDisplayed()
        // Email label and input
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        // Buttons and prompt elements
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter prompt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apply").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
    }

    @Test
    fun background_prompt_applies_blue_background() {
        composeTestRule.setContent {
            CWTheme { SimpleScreen(processor = MockedPromptProcessor.fromJson(mockedJson)) }
        }
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Make background blue")
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithTag("backgroundBox").assert(hasContentDescription("bg:BLUE"))
    }

    @Test
    fun card_prompt_displays_profile_card_title() {
        composeTestRule.setContent {
            CWTheme { SimpleScreen(processor = MockedPromptProcessor.fromJson(mockedJson)) }
        }
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Add a profile card")
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNode(hasTestTag("card") and hasText("Profile")).assertIsDisplayed()
    }

    @Test
    fun title_prompt_sets_title_text() {
        composeTestRule.setContent {
            CWTheme { SimpleScreen(processor = MockedPromptProcessor.fromJson(mockedJson)) }
        }
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Show title My Profile")
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithTag("titleText").assert(hasText("My Profile"))
    }

    @Test
    fun button_prompt_shows_extra_action_button() {
        composeTestRule.setContent {
            CWTheme { SimpleScreen(processor = MockedPromptProcessor.fromJson(mockedJson)) }
        }
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Add Save button")
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNode(hasTestTag("actionButton") and hasText("Save")).assertIsDisplayed()
    }

    @Test
    fun reset_prompt_clears_applied_changes() {
        composeTestRule.setContent {
            CWTheme { SimpleScreen(processor = MockedPromptProcessor.fromJson(mockedJson)) }
        }
        // Apply some changes first
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Make background blue")
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithTag("backgroundBox").assert(hasContentDescription("bg:BLUE"))
        // Now reset
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Reset")
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithTag("backgroundBox").assert(hasContentDescription("bg:UNKNOWN"))
        composeTestRule.onAllNodesWithTag("card").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("actionButton").assertCountEquals(0)
    }
}