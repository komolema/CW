package com.example.cw

import com.example.cw.model.UiComponent
import com.example.cw.model.UiDescription
import com.example.cw.render.ColorName
import com.example.cw.render.RenderBackground
import com.example.cw.render.RenderButton
import com.example.cw.render.RenderCard
import com.example.cw.render.RenderTitle
import com.example.cw.render.UiToRenderModelMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiToRenderModelTest {

    @Test
    fun testBackgroundMapping() {
        val ui = UiDescription(components = listOf(UiComponent(type = "background", color = "blue")))
        val result = UiToRenderModelMapper.map(ui)
        assertEquals(1, result.size)
        val bg = result[0] as RenderBackground
        assertEquals(ColorName.BLUE, bg.color)
    }

    @Test
    fun testTitleMapping() {
        val ui = UiDescription(components = listOf(UiComponent(type = "title", text = "My Profile")))
        val result = UiToRenderModelMapper.map(ui)
        assertEquals(1, result.size)
        val title = result[0] as RenderTitle
        assertEquals("My Profile", title.text)
    }

    @Test
    fun testCardMapping() {
        val ui = UiDescription(
            components = listOf(
                UiComponent(type = "card", properties = mapOf("title" to "Profile", "backgroundColor" to "white"))
            )
        )
        val result = UiToRenderModelMapper.map(ui)
        assertEquals(1, result.size)
        val card = result[0] as RenderCard
        assertEquals("Profile", card.title)
        assertEquals(ColorName.WHITE, card.background)
    }

    @Test
    fun testButtonMapping() {
        val ui = UiDescription(
            components = listOf(UiComponent(type = "button", text = "Save", action = "saveProfile"))
        )
        val result = UiToRenderModelMapper.map(ui)
        assertEquals(1, result.size)
        val btn = result[0] as RenderButton
        assertEquals("Save", btn.text)
        assertEquals("saveProfile", btn.action)
    }

    @Test
    fun testUnknownTypeIgnored() {
        val ui = UiDescription(components = listOf(UiComponent(type = "unknown")))
        val result = UiToRenderModelMapper.map(ui)
        assertTrue(result.isEmpty())
    }
}
