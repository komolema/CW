package com.example.cw

import com.example.cw.model.UiComponent
import com.example.cw.model.UiDescription
import com.example.cw.prompt.PromptProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UiModelTest {

    @Test
    fun testModelInstantiation() {
        val ui = UiDescription(
            components = listOf(
                UiComponent(type = "title", text = "My Profile"),
                UiComponent(type = "background", color = "blue"),
                UiComponent(type = "card", properties = mapOf("title" to "Profile", "backgroundColor" to "white")),
                UiComponent(type = "button", text = "Save", action = "saveProfile")
            )
        )

        assertEquals(4, ui.components.size)
        assertEquals("title", ui.components[0].type)
        assertEquals("My Profile", ui.components[0].text)
        assertEquals("background", ui.components[1].type)
        assertEquals("blue", ui.components[1].color)
        assertEquals("card", ui.components[2].type)
        assertEquals("Profile", ui.components[2].properties["title"])
        assertEquals("white", ui.components[2].properties["backgroundColor"])
        assertEquals("button", ui.components[3].type)
        assertEquals("Save", ui.components[3].text)
        assertEquals("saveProfile", ui.components[3].action)
    }

    @Test
    fun testOptionalFieldsDefaults() {
        val comp = UiComponent(type = "title")
        assertEquals("title", comp.type)
        assertNull(comp.text)
        assertNull(comp.color)
        assertEquals(emptyMap<String, String>(), comp.properties)
        assertNull(comp.action)
    }

    @Test
    fun testPromptProcessorInterface() {
        val processor: PromptProcessor = PromptProcessor { prompt ->
            // For Phase 0, return empty description; implementations come next phases
            UiDescription()
        }
        val result = processor.processPrompt("anything")
        assertEquals(0, result.components.size)
    }
}
