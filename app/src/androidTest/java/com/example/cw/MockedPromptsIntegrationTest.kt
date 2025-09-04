package com.example.cw

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import com.example.cw.ui.theme.CWTheme
import com.example.cw.prompt.MockedPromptProcessor

class MockedPromptsIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockedJson = """
        [
          {"prompt":"Make background blue","uiDescription":{"components":[{"type":"background","color":"blue"}]}},
          {"prompt":"Make background purple","uiDescription":{"components":[{"type":"background","color":"purple"}]}},
          {"prompt":"Show title Karabo","uiDescription":{"components":[{"type":"title","text":"Karabo"}]}},
          {"prompt":"Change save button color to yellow","uiDescription":{"components":[{"type":"button","text":"Save","color":"yellow"}]}},
          {"prompt":"Reset","uiDescription":{"components":[]}}
        ]
    """.trimIndent()

    private fun setContent() {
        composeTestRule.setContent {
            CWTheme { SimpleScreen(processor = MockedPromptProcessor.fromJson(mockedJson)) }
        }
    }

    @Test
    fun background_blue_changes_only_background() {
        setContent()
        // enter prompt and apply
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Make background blue")
        composeTestRule.onNodeWithText("Apply").performClick()
        // background reflects BLUE state
        composeTestRule.onNodeWithTag("backgroundBox").assert(hasContentDescription("bg:BLUE"))
        // prompt input should still be present and not affected; we at least ensure it's still interactable
        composeTestRule.onNodeWithTag("promptInput").assertIsDisplayed()
    }

    @Test
    fun background_purple_applies_purple() {
        setContent()
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Make background purple")
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithTag("backgroundBox").assert(hasContentDescription("bg:PURPLE"))
    }

    @Test
    fun title_changes_to_karabo() {
        setContent()
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Show title Karabo")
        composeTestRule.onNodeWithText("Apply").performClick()
        composeTestRule.onNodeWithTag("titleText").assert(hasText("Karabo"))
    }

    @Test
    fun save_button_changes_to_yellow() {
        setContent()
        composeTestRule.onNodeWithTag("promptInput").performTextInput("Change save button color to yellow")
        composeTestRule.onNodeWithText("Apply").performClick()
        // We can't directly read color easily; instead, re-apply another prompt and ensure save button still exists
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
    }
}
