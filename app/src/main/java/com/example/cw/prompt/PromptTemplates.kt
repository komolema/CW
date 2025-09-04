package com.example.cw.prompt

/**
 * Phase 5 — LLM Prompt Design
 *
 * Provides provider-specific prompt templates that instruct the LLM to emit a UiDescription JSON
 * matching our schema. These are plain template builders; real API integration is intentionally
 * out-of-scope for now.
 */
object PromptTemplates {

    /**
     * OpenAI Chat Completions style prompts.
     * Usage example (pseudo):
     *   val p = PromptTemplates.buildOpenAiPrompt(userPrompt)
     *   openAi.chat(model = "gpt-4o-mini", messages = listOf(
     *       SystemMessage(p.system),
     *       UserMessage(p.user)
     *   ))
     */
    data class OpenAiPrompt(val system: String, val user: String)

    /**
     * Gemini style prompts. Depending on SDK, `instruction` maps to system/role/instructions and
     * `user` is the end-user input content.
     */
    data class GeminiPrompt(val instruction: String, val user: String)

    /** Allowed color names for consistency with UiToRenderModelMapper. */
    private val allowedColors = listOf("blue", "green", "white", "red", "black", "purple", "yellow")

    private fun sharedInstructionBody(): String = buildString {
        appendLine("You are a UI layout planner. Convert natural language requests into a JSON object that matches this schema exactly:")
        appendLine()
        appendLine("Schema:")
        appendLine("{")
        appendLine("  \"components\": [")
        appendLine("    {")
        appendLine("      \"type\": string  // one of: title, background, card, button")
        appendLine("      // For type=title:     use: { \"type\": \"title\", \"text\": string }")
        appendLine("      // For type=background: use: { \"type\": \"background\", \"color\": string }")
        appendLine("      // For type=card:      use: { \"type\": \"card\", \"properties\": { \"title\": string?, \"backgroundColor\": string? } }")
        appendLine("      // For type=button:    use: { \"type\": \"button\", \"text\": string, \"action\": string? }")
        appendLine("    }")
        appendLine("  ]")
        appendLine("}")
        appendLine()
        appendLine("Rules:")
        appendLine("- Output MUST be strictly valid JSON, UTF-8, with double-quoted keys and strings. No markdown fences, no comments, no trailing text.")
        appendLine("- Only include fields defined above. Omit null fields entirely.")
        appendLine("- type values are lowercase: title, background, card, button.")
        appendLine("- color and backgroundColor must be one of: ${allowedColors.joinToString()}. If unknown, prefer \"white\".")
        appendLine("- If the prompt is ambiguous, make the minimal reasonable choice. Do not invent unrelated components.")
        appendLine("- If the prompt asks to reset, return { \"components\": [] }.")
        appendLine()
        appendLine("Few-shot Examples:")
        appendLine("User: Make background blue")
        appendLine("Response:")
        appendLine("{\"components\":[{\"type\":\"background\",\"color\":\"blue\"}]}")
        appendLine()
        appendLine("User: Show title My Profile")
        appendLine("Response:")
        appendLine("{\"components\":[{\"type\":\"title\",\"text\":\"My Profile\"}]}")
        appendLine()
        appendLine("User: Add a profile card")
        appendLine("Response:")
        appendLine("{\"components\":[{\"type\":\"card\",\"properties\":{\"title\":\"Profile\",\"backgroundColor\":\"white\"}}]}")
        appendLine()
        appendLine("User: Add Save button")
        appendLine("Response:")
        appendLine("{\"components\":[{\"type\":\"button\",\"text\":\"Save\",\"action\":\"saveProfile\"}]}")
    }

    fun buildOpenAiPrompt(userPrompt: String): OpenAiPrompt {
        val system = buildString {
            appendLine(sharedInstructionBody())
            appendLine()
            appendLine("When you answer, respond with JSON only. Do not wrap with ```.")
        }.trim()
        val user = userPrompt.trim()
        return OpenAiPrompt(system = system, user = user)
    }

    fun buildGeminiPrompt(userPrompt: String): GeminiPrompt {
        val instruction = buildString {
            appendLine(sharedInstructionBody())
            appendLine()
            appendLine("Return JSON only. No backticks. No prose.")
        }.trim()
        val user = userPrompt.trim()
        return GeminiPrompt(instruction = instruction, user = user)
    }
}
