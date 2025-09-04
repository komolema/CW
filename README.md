# Mobile UI Playground (CW)

This app demonstrates a modular UI description → rendering pipeline, with a mocked prompt processor and optional LLM prompt templates.

Status: Phase 6 — Polish and Observability complete.

How to run:
- Open in Android Studio (Giraffe+), sync Gradle.
- Run the app on an emulator/device.

How to use:
- Type a prompt in the input field and tap Apply.
- Or tap one of the suggestion buttons.
- Supported example prompts (case-insensitive):
  - "Make background blue"
  - "Make background purple"
  - "Add a profile card"
  - "Show title Karabo"
  - "Add Save button"
  - "Change save button color to yellow"
  - "Reset" (resets to initial UI)

Prompt source:
- MockedPromptProcessor reads predefined mappings from `app/src/main/assets/prompts.json`.
- Matching is case-insensitive and ignores surrounding whitespace.
- You can edit that JSON to add your own prompt→UI mappings; unknown prompts result in an empty layout with a Snackbar.

Optional LLM mode and provider switcher:
- A simple provider switcher UI is available at the top of the screen (Mocked, Gemini).
- Defaults: Gemini defaults to `gemini-1.5-flash` (free-tier). You can edit the model name in the UI.
- Enter the API key when selecting Gemini.
- The current app ships with a placeholder LLM client that returns null and gracefully falls back to the mocked prompts until you integrate a real provider SDK.
- PromptTemplates.kt contains a provider-specific prompt template for Gemini to instruct an LLM to emit the same UiDescription JSON schema.
- LLMPromptProcessor wires in the provider and falls back to the mocked processor on error or when disabled.

Observability:
- The app logs prompt→result mapping and errors via DebugLog with a `[DEBUG_LOG]` prefix.
- Unknown prompts trigger a Snackbar notification and a log entry.
- Malformed assets or JSON parsing errors are logged and the app falls back to an empty prompt map.

Tests:
- JVM unit tests cover models, mapping, prompt templates, and state store.
- Android instrumented tests validate basic rendering behaviors.

Screenshots:
- See `ui.png` for a conceptual layout.
