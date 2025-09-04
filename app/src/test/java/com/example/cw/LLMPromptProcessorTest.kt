package com.example.cw

import com.example.cw.model.UiDescription
import com.example.cw.prompt.LLMPromptProcessor
import com.example.cw.prompt.PromptProcessor
import org.junit.Assert.assertEquals
import org.junit.Test

class LLMPromptProcessorTest {

    private class FakeClient(private val response: String?) : LLMPromptProcessor.LLMClient {
        override fun generate(
            provider: LLMPromptProcessor.Provider,
            systemOrInstruction: String,
            user: String
        ): String? = response
    }

    @Test
    fun wellFormedJson_returnsUiDescription() {
        val json = """
            {"components":[{"type":"title","text":"From LLM"},{"type":"background","color":"blue"}]}
        """.trimIndent()
        val client = FakeClient(json)
        val fallback = PromptProcessor { UiDescription() }
        val proc = LLMPromptProcessor(client, fallback, provider = LLMPromptProcessor.Provider.OpenAI, enabled = true)
        val ui = proc.processPrompt("any")
        assertEquals(2, ui.components.size)
        assertEquals("title", ui.components[0].type)
        assertEquals("From LLM", ui.components[0].text)
    }

    @Test
    fun errorOrBlank_fallsBackToMocked() {
        val client = FakeClient("")
        val fallbackUi = UiDescription()
        val fallback = PromptProcessor { fallbackUi }
        val proc = LLMPromptProcessor(client, fallback, provider = LLMPromptProcessor.Provider.Gemini, enabled = true)
        val ui = proc.processPrompt("x")
        assertEquals(fallbackUi, ui)
    }

    @Test
    fun disabledFlag_delegatesToFallback() {
        val client = FakeClient(null)
        val fallbackUi = UiDescription()
        val fallback = PromptProcessor { fallbackUi }
        val proc = LLMPromptProcessor(client, fallback, enabled = false)
        val ui = proc.processPrompt("x")
        assertEquals(fallbackUi, ui)
    }
}
