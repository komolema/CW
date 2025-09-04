package com.example.cw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.cw.model.UiDescription
import com.example.cw.prompt.MockedPromptProcessor
import com.example.cw.prompt.PromptProcessor
import com.example.cw.prompt.LLMPromptProcessor
import com.example.cw.ui.theme.CWTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CWTheme {
                val context = LocalContext.current

                // Provider selection + credentials state
                var provider by remember { mutableStateOf("MOCK") } // MOCK, GEMINI
                var geminiKey by remember { mutableStateOf("") }
                var geminiModel by remember { mutableStateOf("gemini-1.5-flash") } // default free-tier

                // Base mocked processor
                val mocked = remember { MockedPromptProcessor.fromAssets(context) }

                // Simple LLM client placeholder; integrate real SDKs later
                val client = remember {
                    object : LLMPromptProcessor.LLMClient {
                        override fun generate(
                            provider: LLMPromptProcessor.Provider,
                            systemOrInstruction: String,
                            user: String
                        ): String? {
                            // Placeholder: In a real integration, use provider SDKs with API keys & model names.
                            // Returning null triggers fallback to mocked prompts.
                            com.example.cw.util.DebugLog.d("LLMClient: called for ${provider}, returning null (mock)")
                            return null
                        }
                    }
                }

                // Compute processor based on selection
                val processor = remember(provider, geminiKey, geminiModel) {
                    when (provider) {
                        "GEMINI" -> LLMPromptProcessor(
                            client = client,
                            fallbackProcessor = mocked,
                            provider = LLMPromptProcessor.Provider.Gemini,
                            enabled = true,
                            modelName = geminiModel,
                            hasKey = geminiKey.isNotBlank()
                        )
                        else -> mocked
                    }
                }

                SettingsAndScreen(
                    provider = provider,
                    onProviderChange = { provider = it },
                    geminiKey = geminiKey,
                    onGeminiKeyChange = { geminiKey = it },
                    geminiModel = geminiModel,
                    onGeminiModelChange = { geminiModel = it },
                    processor = processor
                )
            }
        }
    }
}

@Composable
fun SettingsAndScreen(
    provider: String,
    onProviderChange: (String) -> Unit,
    geminiKey: String,
    onGeminiKeyChange: (String) -> Unit,
    geminiModel: String,
    onGeminiModelChange: (String) -> Unit,
    processor: PromptProcessor
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Collapsible provider & credentials block using Material patterns
        var expanded by remember { mutableStateOf(true) }
        androidx.compose.material3.ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "providerCardHeader" }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provider")
                }
                Button(onClick = { expanded = !expanded }, colors = ButtonDefaults.textButtonColors()) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            if (expanded) {
                var testResult by remember { mutableStateOf<String?>(null) }
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onProviderChange("MOCK") }) { Text("Mocked") }
                        Button(onClick = { onProviderChange("GEMINI") }) { Text("Gemini") }
                    }
                    when (provider) {
                        "GEMINI" -> {
                            OutlinedTextField(
                                value = geminiKey,
                                onValueChange = onGeminiKeyChange,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("geminiKey"),
                                label = { Text("Gemini API Key") }
                            )
                            OutlinedTextField(
                                value = geminiModel,
                                onValueChange = onGeminiModelChange,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("geminiModel"),
                                label = { Text("Gemini Model (default: gemini-1.5-flash)") }
                            )
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                testResult?.let { Text(it) }
                                val scope = androidx.compose.runtime.rememberCoroutineScope()
                                Button(onClick = {
                                    if (geminiKey.isBlank()) {
                                        testResult = "Failed: Missing API key"
                                        // Clear any existing input (already blank), keep consistent behavior
                                        onGeminiKeyChange("")
                                        com.example.cw.util.DebugLog.d("TestConnection: Gemini — failed (missing key), cleared API key input")
                                    } else {
                                        testResult = "Testing…"
                                        scope.launch {
                                            try {
                                                val (ok, msg) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    com.example.cw.prompt.GeminiHttp.testConnection(geminiKey.trim(), geminiModel.trim())
                                                }
                                                testResult = msg
                                                if (ok) {
                                                    com.example.cw.util.DebugLog.d("TestConnection: Gemini — success (model='${'$'}geminiModel')")
                                                } else {
                                                    // Clear API key on failure as requested
                                                    onGeminiKeyChange("")
                                                    com.example.cw.util.DebugLog.d("TestConnection: Gemini — failed: ${'$'}msg; cleared API key input")
                                                }
                                            } catch (e: Exception) {
                                                val em = e.message ?: e::class.java.simpleName
                                                testResult = "Failed: ${'$'}em"
                                                com.example.cw.util.DebugLog.d("TestConnection: Gemini — exception: ${'$'}em")
                                            }
                                        }
                                    }
                                }) { Text("Test connection") }
                            }
                        }
                    }
                }
            }
        }
        // Divider-like spacing
        androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 8.dp))
        // Main screen below
        SimpleScreen(processor = processor)
    }
}

@Composable
fun SimpleScreen(processor: PromptProcessor) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }

    // UI state driven by mocked prompts
    var titleText by remember { mutableStateOf("My Profile") }
    var backgroundName by remember { mutableStateOf("UNKNOWN") } // BLUE, GREEN, WHITE, RED, BLACK, UNKNOWN
    var cardTitle by remember { mutableStateOf<String?>(null) }
    var extraButtonText by remember { mutableStateOf<String?>(null) }
    var saveButtonColorName by remember { mutableStateOf("DEFAULT") } // DEFAULT uses MaterialTheme default; else color name like YELLOW
    var unknownPromptMessage by remember { mutableStateOf<String?>(null) }

    fun applyUi(ui: com.example.cw.model.UiDescription) {
        com.example.cw.util.DebugLog.d("Apply: starting apply ${ui.components.size} components")
        if (ui.components.isEmpty()) {
            // Reset to defaults or unknown prompt feedback handled elsewhere
            titleText = "My Profile"
            backgroundName = "UNKNOWN"
            cardTitle = null
            extraButtonText = null
            saveButtonColorName = "DEFAULT"
            return
        }
        // Apply all components in order; last of each type wins (simple strategy)
        ui.components.forEach { c ->
            unknownPromptMessage = null
            when (c.type.lowercase()) {
                "background" -> {
                    val before = backgroundName
                    backgroundName = when (c.color?.lowercase()) {
                        "blue" -> "BLUE"
                        "green" -> "GREEN"
                        "white" -> "WHITE"
                        "red" -> "RED"
                        "black" -> "BLACK"
                        "purple" -> "PURPLE"
                        else -> "UNKNOWN"
                    }
                    com.example.cw.util.DebugLog.d("Apply: background from ${before} -> ${backgroundName}")
                }
                "title" -> c.text?.let { titleText = it; com.example.cw.util.DebugLog.d("Apply: title='${it}'") }
                "card" -> {
                    val t = c.properties["title"]
                    cardTitle = t
                    com.example.cw.util.DebugLog.d("Apply: card title='${t}'")
                }
                "button" -> {
                    val t = c.text ?: ""
                    val col = c.color?.trim()?.lowercase()
                    if (t.equals("Save", ignoreCase = true) && !col.isNullOrEmpty()) {
                        saveButtonColorName = col.uppercase()
                        com.example.cw.util.DebugLog.d("Apply: save button color='${saveButtonColorName}'")
                    } else {
                        extraButtonText = t
                        com.example.cw.util.DebugLog.d("Apply: extra button text='${t}'")
                    }
                }
            }
        }
    }

    @Composable
    fun colorFor(name: String): androidx.compose.ui.graphics.Color = when (name) {
        "BLUE" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
        "GREEN" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "WHITE" -> androidx.compose.ui.graphics.Color.White
        "RED" -> androidx.compose.ui.graphics.Color(0xFFF44336)
        "BLACK" -> androidx.compose.ui.graphics.Color.Black
        "PURPLE" -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
        else -> androidx.compose.material3.MaterialTheme.colorScheme.background
    }

    @Composable
    fun buttonColorFor(name: String): androidx.compose.ui.graphics.Color = when (name) {
        "YELLOW" -> androidx.compose.ui.graphics.Color(0xFFFFEB3B)
        "BLUE" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
        "GREEN" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "RED" -> androidx.compose.ui.graphics.Color(0xFFF44336)
        "BLACK" -> androidx.compose.ui.graphics.Color.Black
        "WHITE" -> androidx.compose.ui.graphics.Color.White
        "PURPLE" -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
        else -> androidx.compose.material3.MaterialTheme.colorScheme.primary
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorFor(backgroundName))
                .semantics { contentDescription = "bg:$backgroundName" }
                .testTag("backgroundBox")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Centered Title
                Text(
                    text = titleText,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.CenterHorizontally)
                        .testTag("titleText")
                )

                // Name
                Text(text = "Name")
                androidx.compose.material3.TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )

                // Email
                Text(text = "Email")
                androidx.compose.material3.TextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )

                // Save button
                Button(
                    onClick = { /* Save name/email if needed */ },
                    colors = if (saveButtonColorName != "DEFAULT") ButtonDefaults.buttonColors(
                        containerColor = buttonColorFor(saveButtonColorName)
                    ) else ButtonDefaults.buttonColors()
                ) { Text("Save") }

                // Optional extra button from mocked prompt
                extraButtonText?.let { btnText ->
                    if (btnText.isNotEmpty()) {
                        Button(onClick = { /* no-op */ }, modifier = Modifier.testTag("actionButton")) { Text(btnText) }
                    }
                }

                // Profile card (improved styling)
                cardTitle?.let { ct ->
                    androidx.compose.material3.Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = ct,
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                modifier = Modifier.testTag("card")
                            )
                            Text(text = "Tap Save to keep changes", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Prompt input (default uses mocked prompts via processor)
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("promptInput"),
                    label = { Text("Enter prompt") },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )

                // Unknown prompt message
                unknownPromptMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("unknownPromptMessage")
                    )
                }

                // Apply and Reset buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        com.example.cw.util.DebugLog.d("UI: Apply clicked with prompt='${prompt.take(80)}'")
                        val ui = processor.processPrompt(prompt)
                        com.example.cw.util.DebugLog.d("UI: processor returned ${ui.components.size} components")
                        val normalized = prompt.trim().lowercase()
                        if (ui.components.isEmpty()) {
                            if (normalized.contains("reset")) {
                                unknownPromptMessage = null
                                applyUi(com.example.cw.model.UiDescription(emptyList()))
                            } else if (normalized.isNotEmpty()) {
                                // Unknown prompt: keep current UI and show message
                                unknownPromptMessage = "Unknown prompt"
                            }
                        } else {
                            unknownPromptMessage = null
                            applyUi(ui)
                        }
                    }) { Text("Apply") }
                    Button(onClick = {
                        prompt = ""
                        unknownPromptMessage = null
                        applyUi(com.example.cw.model.UiDescription(emptyList()))
                    }) { Text("Reset") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    CWTheme {
        SimpleScreen(processor = PromptProcessor { UiDescription() })
    }
}