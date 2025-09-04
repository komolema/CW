package com.example.cw.prompt

import com.example.cw.util.DebugLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal, dependency-free HTTP client for Google Generative Language API (Gemini) v1beta.
 * This is intentionally simple and synchronous; call from a background thread.
 */
object GeminiHttp {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

    data class ModelInfo(val name: String)

    /**
     * Lists available models for the provided API key. Returns model names; on error returns empty list.
     */
    fun listModels(apiKey: String, timeoutMs: Int = 10_000): List<ModelInfo> {
        return try {
            val urlStr = "$BASE_URL/models?key=$apiKey"
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream?.let { s ->
                BufferedReader(InputStreamReader(s, Charsets.UTF_8)).use { it.readText() }
            }
            if (code !in 200..299) {
                DebugLog.d("Gemini listModels failed (HTTP $code): ${response?.replace("\n"," ")?.take(140) ?: ""}")
                emptyList()
            } else {
                val root = Json.parseToJsonElement(response ?: "{}").jsonObject
                val arr = root["models"]?.jsonArray
                val list = arr?.mapNotNull { el ->
                    val obj = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
                    val name = obj["name"]?.jsonPrimitive?.content
                    name?.let { ModelInfo(it.substringAfter("models/")) }
                } ?: emptyList()
                list
            }
        } catch (e: Exception) {
            DebugLog.d("Gemini listModels error: ${e::class.java.name}: ${e.message}\n" + e.stackTrace.joinToString("\n") { "    at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" })
            emptyList()
        }
    }

    /**
     * Picks a recommended free-tier model name from the provided list.
     * Priority order is based on current public docs assumptions.
     */
    fun recommendFreeModel(models: List<ModelInfo>): String {
        val names = models.map { it.name }
        val priority = listOf(
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-2.0-flash-exp",
            "gemini-1.5-flash-8b",
            "gemini-1.5-flash"
        )
        for (p in priority) {
            if (names.contains(p)) return p
        }
        // Fallback: any flash model
        names.firstOrNull { it.contains("flash") }?.let { return it }
        // Ultimate fallback: keep our default
        return "gemini-2.0-flash"
    }

    /**
     * Performs a lightweight request to validate the API key and model. Returns Pair(success, message).
     */
    fun testConnection(apiKey: String, model: String, timeoutMs: Int = 10_000): Pair<Boolean, String> {
        return try {
            // We call generateContent with a trivial prompt; any 2xx means the key+model are acceptable.
            val res = generateContent(apiKey, model, instruction = "You are a JSON echoer.", user = "Return {\"components\":[]}", timeoutMs)
            if (res.httpCode in 200..299) {
                true to "Connected (HTTP ${'$'}{res.httpCode})"
            } else {
                val brief = when (res.httpCode) {
                    400 -> "Bad request. Check model name."
                    401, 403 -> "Invalid API key or unauthorized."
                    404 -> "Model not found."
                    429 -> "Rate limited. Try later."
                    else -> null
                }
                val raw = (res.errorMessage ?: "")
                val trimmed = raw.replace("\n", " ").take(140)
                val suffix = if (trimmed.isNotBlank()) ": $trimmed" else ""
                false to ("Failed (HTTP ${res.httpCode})" + (brief?.let { ": $it" } ?: "") + suffix)
            }
        } catch (e: Exception) {
            DebugLog.d("Gemini testConnection error: ${e::class.java.name}: ${e.message}\n" + e.stackTrace.joinToString("\n") { "    at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" })
            false to ("Failed: ${e::class.java.simpleName}: ${e.message}")
        }
    }

    data class GenerateResult(val httpCode: Int, val text: String?, val errorMessage: String?)

    /**
     * Calls models/{model}:generateContent and returns the first candidate text if available.
     */
    fun generateContent(apiKey: String, model: String, instruction: String, user: String, timeoutMs: Int = 20_000): GenerateResult {
        val urlStr = "$BASE_URL/models/$model:generateContent?key=$apiKey"
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = buildString {
            append('{')
            append("\"contents\":[{\"parts\":[{\"text\":")
            append(jsonEscape(user))
            append("}]}],")
            append("\"systemInstruction\":{\"parts\":[{\"text\":")
            append(jsonEscape(instruction))
            append("}]}\n")
            append('}')
        }
        BufferedWriter(OutputStreamWriter(conn.outputStream, Charsets.UTF_8)).use { it.write(body) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.let { s ->
            BufferedReader(InputStreamReader(s, Charsets.UTF_8)).use { it.readText() }
        }
        if (code !in 200..299) {
            return GenerateResult(code, null, response)
        }
        // Parse candidates[0].content.parts[0].text
        return try {
            val json = Json.parseToJsonElement(response ?: "{}").jsonObject
            val candidates = json["candidates"]?.jsonArray
            val first = candidates?.firstOrNull()?.jsonObject
            val content = first?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
            GenerateResult(code, text, null)
        } catch (e: Exception) {
            DebugLog.d("Gemini parse error: ${e::class.java.name}: ${e.message}\n" + e.stackTrace.joinToString("\n") { "    at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" })
            GenerateResult(code, null, "Parse error: ${e.message}")
        } finally {
            conn.disconnect()
        }
    }

    private fun jsonEscape(text: String): String {
        // basic JSON string escape
        val escaped = text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return "\"$escaped\""
    }
}
