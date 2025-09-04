package com.example.cw

import com.example.cw.prompt.MockedPromptProcessor
import com.example.cw.util.DebugLog
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugLoggingTest {

    private val json = """
        [
          {
            "prompt": "Make background blue",
            "uiDescription": {
              "components": [ { "type": "background", "color": "blue" } ]
            }
          }
        ]
    """.trimIndent()

    @Test
    fun logs_on_known_and_unknown_prompts() {
        DebugLog.clear()
        val processor = MockedPromptProcessor.fromJson(json)

        processor.processPrompt("Make background blue")
        processor.processPrompt("unknown")

        val entries = DebugLog.entries
        assertTrue(entries.any { it.contains("[DEBUG_LOG]") && it.contains("matched") })
        assertTrue(entries.any { it.contains("[DEBUG_LOG]") && it.contains("unknown") })
    }
}
