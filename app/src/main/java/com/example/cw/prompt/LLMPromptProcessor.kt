package com.example.cw.prompt

import com.example.cw.model.UiComponent
import com.example.cw.model.UiDescription
import com.example.cw.util.DebugLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Phase 5 — LLM-backed PromptProcessor (feature-flagged)
 *
 * This implementation uses PromptTemplates to construct provider-specific prompts and expects
 * the provider SDK (not included) to return a raw JSON string for UiDescription.
 * For this challenge, we keep it provider-agnostic by injecting a simple LLMClient interface
 * that can be faked in tests. The processor parses the JSON and falls back to the
 * provided fallbackProcessor on any error.
 */
class LLMPromptProcessor(
    private val client: LLMClient,
    private val fallbackProcessor: PromptProcessor,
    private val provider: Provider = Provider.OpenAI,
    private val enabled: Boolean = false,
    private val modelName: String? = null,
    private val hasKey: Boolean? = null
) : PromptProcessor {

    enum class Provider { OpenAI, Gemini }

    interface LLMClient {
        /**
         * Given a provider and the input prompt strings, return a raw JSON string representing
         * UiDescription per our schema. Throw or return null/blank on failure.
         */
        fun generate(provider: Provider, systemOrInstruction: String, user: String): String?
    }

    private val json = Json { ignoreUnknownKeys = true }

    override fun processPrompt(promptText: String): UiDescription {
        if (!enabled) {
            DebugLog.d("LLMPromptProcessor: feature disabled, delegating to fallback")
            return fallbackProcessor.processPrompt(promptText)
        }
        return try {
            val (sysOrInstr, user) = when (provider) {
                Provider.OpenAI -> {
                    val p = PromptTemplates.buildOpenAiPrompt(promptText)
                    p.system to p.user
                }
                Provider.Gemini -> {
                    val p = PromptTemplates.buildGeminiPrompt(promptText)
                    p.instruction to p.user
                }
            }
            val reqId = System.currentTimeMillis().toString(16)
            val ctx = buildString {
                append("provider=").append(provider)
                modelName?.let { append(", model='").append(it).append("'") }
                hasKey?.let { append(", hasKey=").append(it) }
            }
            DebugLog.d("LLM Request: reqId=${reqId}, ${ctx}, userPrompt='${promptText.take(120)}', sysOrInstrLen=${sysOrInstr.length}, userLen=${user.length}")
            val t0 = System.nanoTime()
            val raw = client.generate(provider, sysOrInstr, user).orEmpty()
            val dtMs = (System.nanoTime() - t0) / 1_000_000
            DebugLog.d("LLM Response: reqId=${reqId}, ${ctx}, tookMs=${dtMs}, rawLen=${raw.length}, preview='${raw.replace("\n"," ").take(160)}'")
            if (raw.isBlank()) error("Empty response from LLM")
            val ui = parseUiDescription(raw)
            DebugLog.d("Parse: reqId=${reqId}, UiDescription with ${ui.components.size} components -> will apply in renderer")
            ui
        } catch (e: Exception) {
            DebugLog.d("LLMPromptProcessor: error '${e.message}', falling back")
            fallbackProcessor.processPrompt(promptText)
        }
    }

    private fun parseUiDescription(rawJson: String): UiDescription {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val comps: JsonArray = (root["components"] ?: JsonArray(emptyList())).jsonArray
        val list = comps.mapNotNull { el ->
            val obj: JsonObject = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
            parseUiComponent(obj)
        }
        return UiDescription(list)
    }

    private fun parseUiComponent(obj: JsonObject): UiComponent? {
        val type = obj["type"]?.jsonPrimitive?.content ?: return null
        val text = obj["text"]?.jsonPrimitive?.content
        val color = obj["color"]?.jsonPrimitive?.content
        val action = obj["action"]?.jsonPrimitive?.content
        val props = (obj["properties"] as? JsonObject)?.let { it.mapValues { (_, v) -> v.jsonPrimitive.content } } ?: emptyMap()
        return UiComponent(type = type, text = text, color = color, properties = props, action = action)
    }
}
