package com.example.cw

import com.example.cw.model.UiDescription
import com.example.cw.prompt.MockedPromptProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockedPromptProcessorTest {

    private val json = """
        [
          {
            "prompt": "Make background blue",
            "uiDescription": {
              "components": [ { "type": "background", "color": "blue" } ]
            }
          },
          {
            "prompt": "Add a profile card",
            "uiDescription": {
              "components": [ { "type": "card", "properties": { "backgroundColor": "white", "title": "Profile" } } ]
            }
          },
          {
            "prompt": "Show title My Profile",
            "uiDescription": {
              "components": [ { "type": "title", "text": "My Profile" } ]
            }
          },
          {
            "prompt": "Add Save button",
            "uiDescription": {
              "components": [ { "type": "button", "text": "Save", "action": "saveProfile" } ]
            }
          },
          {
            "prompt": "Reset",
            "uiDescription": {
              "components": []
            }
          }
        ]
    """.trimIndent()

    @Test
    fun testExactMatchReturnsExpectedUiDescription() {
        val processor = MockedPromptProcessor.fromJson(json)
        val result = processor.processPrompt("Make background blue")
        assertEquals(1, result.components.size)
        val comp = result.components[0]
        assertEquals("background", comp.type)
        assertEquals("blue", comp.color)
    }

    @Test
    fun testCaseInsensitiveMatch() {
        val processor = MockedPromptProcessor.fromJson(json)
        val result = processor.processPrompt("make BACKGROUND blue")
        assertEquals(1, result.components.size)
        assertEquals("background", result.components[0].type)
        assertEquals("blue", result.components[0].color)
    }

    @Test
    fun testUnknownPromptReturnsEmptyDescription() {
        val processor = MockedPromptProcessor.fromJson(json)
        val result = processor.processPrompt("unknown command")
        assertTrue(result.components.isEmpty())
    }

    @Test
    fun testJsonParsingForAllExamples() {
        val processor = MockedPromptProcessor.fromJson(json)

        val title = processor.processPrompt("Show title My Profile")
        assertEquals(1, title.components.size)
        assertEquals("title", title.components[0].type)
        assertEquals("My Profile", title.components[0].text)

        val card = processor.processPrompt("Add a profile card")
        assertEquals(1, card.components.size)
        assertEquals("card", card.components[0].type)
        assertEquals("Profile", card.components[0].properties["title"])
        assertEquals("white", card.components[0].properties["backgroundColor"])

        val button = processor.processPrompt("Add Save button")
        assertEquals(1, button.components.size)
        assertEquals("button", button.components[0].type)
        assertEquals("Save", button.components[0].text)
        assertEquals("saveProfile", button.components[0].action)
    }
}
