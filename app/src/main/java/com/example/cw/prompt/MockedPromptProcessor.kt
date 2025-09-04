package com.example.cw.prompt

import android.content.Context
import com.example.cw.model.UiComponent
import com.example.cw.model.UiDescription
import com.example.cw.util.DebugLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mocked implementation of PromptProcessor that reads predefined prompt→UiDescription
 * pairs from JSON. Matching is case-insensitive and ignores surrounding whitespace.
 *
 * JSON schema (array):
 * [
 *   { "prompt": "Make background blue", "uiDescription": { "components": [ { "type": "background", "color": "blue" } ] } }
 * ]
 */
class MockedPromptProcessor private constructor(
    private val promptMap: Map<String, UiDescription>
) : PromptProcessor {

    override fun processPrompt(promptText: String): UiDescription {
        val key = normalize(promptText)
        val result = promptMap[key]
        return if (result != null) {
            DebugLog.d("MockedPromptProcessor: prompt='$promptText' → matched (${result.components.size} components)")
            result
        } else {
            DebugLog.d("MockedPromptProcessor: prompt='$promptText' → unknown (0 components)")
            UiDescription(emptyList())
        }
    }

    private fun normalize(s: String): String = s.trim().lowercase()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(jsonString: String): MockedPromptProcessor {
            return try {
                val root = json.parseToJsonElement(jsonString)
                val array: JsonArray = root.jsonArray
                val map = buildMap<String, UiDescription> {
                    for (entry in array) {
                        val obj: JsonObject = entry.jsonObject
                        val prompt = obj["prompt"]?.jsonPrimitive?.content
                        val uiDescObj = obj["uiDescription"]?.jsonObject
                        if (prompt == null || uiDescObj == null) {
                            DebugLog.d("MockedPromptProcessor: skipping malformed entry: $obj")
                            continue
                        }
                        val ui = parseUiDescription(uiDescObj)
                        put(normalize(prompt), ui)
                    }
                }
                DebugLog.d("MockedPromptProcessor: loaded ${'$'}{map.size} prompts from JSON")
                MockedPromptProcessor(map)
            } catch (e: Exception) {
                DebugLog.d("MockedPromptProcessor: error parsing JSON: ${'$'}{e.message}")
                MockedPromptProcessor(emptyMap())
            }
        }

        fun fromAssets(context: Context, assetPath: String = "prompts.json"): MockedPromptProcessor {
            return try {
                val jsonString = context.assets.open(assetPath).use { it.readBytes().decodeToString() }
                fromJson(jsonString)
            } catch (e: Exception) {
                DebugLog.d("MockedPromptProcessor: error loading assets/${'$'}assetPath: ${'$'}{e.message}")
                MockedPromptProcessor(emptyMap())
            }
        }

        private fun parseUiDescription(obj: JsonObject): UiDescription {
            val compsArray = obj["components"]?.jsonArray ?: JsonArray(emptyList())
            val components = compsArray.mapNotNull { compEl ->
                val comp = runCatching { parseUiComponent(compEl.jsonObject) }.getOrNull()
                if (comp == null) DebugLog.d("MockedPromptProcessor: ignoring unknown/malformed component: ${'$'}compEl")
                comp
            }
            return UiDescription(components)
        }

        private fun parseUiComponent(obj: JsonObject): UiComponent? {
            val type = obj["type"]?.jsonPrimitive?.content ?: return null
            val text = obj["text"]?.jsonPrimitive?.content
            val color = obj["color"]?.jsonPrimitive?.content
            val action = obj["action"]?.jsonPrimitive?.content
            val propsObj = obj["properties"] as? JsonObject
            val properties = propsObj?.let { jsonObjectToMap(it) } ?: emptyMap()
            return UiComponent(type = type, text = text, color = color, properties = properties, action = action)
        }

        private fun jsonObjectToMap(obj: JsonObject): Map<String, String> = obj.mapValues { (_, v) ->
            when {
                v is JsonObject -> v.toString()
                v is JsonArray -> v.toString()
                else -> v.jsonPrimitive.content
            }
        }

        private fun normalize(s: String): String = s.trim().lowercase()
    }
}
