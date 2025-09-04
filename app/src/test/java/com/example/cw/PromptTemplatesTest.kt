package com.example.cw

import com.example.cw.prompt.PromptTemplates
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTemplatesTest {

    @Test
    fun openAiPrompt_contains_schema_rules_examples_and_user_text() {
        val user = "Make background blue  "
        val p = PromptTemplates.buildOpenAiPrompt(user)
        // System should include schema cues and rules
        assertTrue(p.system.contains("\"components\""))
        assertTrue(p.system.contains("Rules:"))
        assertTrue(p.system.contains("Few-shot Examples:"))
        assertTrue(p.system.contains("JSON only"))
        // User should be trimmed and preserved
        assertTrue(p.user == "Make background blue")
        assertFalse(p.user.contains("  "))
    }

    @Test
    fun geminiPrompt_contains_schema_rules_examples_and_user_text() {
        val user = "Add Save button"
        val p = PromptTemplates.buildGeminiPrompt(user)
        // Instruction should include schema cues and rules
        assertTrue(p.instruction.contains("\"components\""))
        assertTrue(p.instruction.contains("Rules:"))
        assertTrue(p.instruction.contains("Few-shot Examples:"))
        assertTrue(p.instruction.contains("JSON only"))
        // User should be preserved as-is (trimmed by caller in our builder)
        assertTrue(p.user == user)
    }
}
