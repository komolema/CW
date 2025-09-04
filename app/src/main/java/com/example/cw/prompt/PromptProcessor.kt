package com.example.cw.prompt

import com.example.cw.model.UiDescription

/**
 * Strategy interface to convert user prompt text into a UiDescription.
 * Implementations: MockedPromptProcessor (required), LLMPromptProcessor (optional).
 */
fun interface PromptProcessor {
    fun processPrompt(promptText: String): UiDescription
}
