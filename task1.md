# Task Plan — Mobile UI Playground (Guided by architecture.md)

Status: Phases 0–6 implemented; Phase 5 completed (feature-flagged LLMPromptProcessor + tests). Updated: 2025-09-04 04:51.

This document breaks the challenge (challenge.md) into granular, verifiable tasks, grouped by themes and phases. It follows John Ousterhout’s principles: deep modules, simple interfaces, information hiding, and pulling complexity downward.

Glossary:
- UiDescription: A data model describing the UI (list of components) independent of Compose.
- UiComponent: Sealed hierarchy representing supported components (title, background, card, button, etc.).
- PromptProcessor: Strategy interface to turn a user prompt into UiDescription. Two strategies: Mocked (required) and LLM (optional bonus).

Acceptance: The plan stops here until approval. After approval, execution proceeds by phases, with tests and verification after each phase.

---

## Phase 0 — Foundations (Planning, Contracts, Tooling)

Theme A: Domain Models and Contracts (Deep, stable interfaces)
- A1. Define data models in Kotlin (module: app):
  - UiDescription(val components: List<UiComponent>)
  - sealed class UiComponent with data classes:
    - Title(text: String)
    - Background(color: String)
    - Card(properties: Map<String, String> or typed props)
    - Button(text: String, action: String)
    - (Optional extensions – TextInput, ToggleSwitch, ProgressBar) kept out of v1 scope; stubs allowed.
  - Why: Deep module decoupling UI description from Compose (Ousterhout: information hiding, pull complexity down).
  - Verifiable by: Unit tests serialize/deserialize JSON ↔ models.
- A2. Define rendering contract:
  - fun renderUI(ui: UiDescription) at a top-level or inside a Renderer object.
  - Renderer hides all Compose specifics from call sites (deep module).
  - Verifiable by: Unit tests verifying mapping function UiComponent → RenderModel (pure) before Compose usage.
- A3. Define prompt processing contract:
  - interface PromptProcessor { fun processPrompt(promptText: String): UiDescription }
  - Implementations planned: MockedPromptProcessor (v1), LLMPromptProcessor (bonus).
  - Verifiable by: Unit tests around PromptProcessor with known inputs.
- A4. Add JSON parser selection:
  - Choose kotlinx.serialization for minimal deps (or Moshi if already present). Add Gradle config.
  - Verifiable by: Unit tests for parsing/encoding using sample JSON from architecture.md.

Theme B: Build/Tooling Setup
- B1. Add Gradle dependencies for kotlinx.serialization and Compose testing if missing.
- B2. Ensure JVM unit tests (test/) and Android instrumented tests (androidTest/) are runnable.
- Verifiable by: ./gradlew test and connectedAndroidTest run green (or via IDE). Place initial smoke tests.

Definition of Done (Phase 0)
- Data classes, interfaces compile.
- Parsing unit tests pass.

---

## Phase 1 — Prompt Processing (Mocked Required)

Theme C: Predefined Prompts and Mock Strategy
- C1. Create assets/prompts.json with entries exactly like architecture.md’s schema and 3–5 examples required by challenge.md.
  - Examples:
    - "Make background blue" → Background(color="blue")
    - "Add a profile card" → Card(properties.title="Profile", backgroundColor="white")
    - "Show title My Profile" → Title(text="My Profile")
    - "Add Save button" → Button(text="Save", action="saveProfile")
    - "Reset" → UiDescription of initial state or special command (see Phase 3).
- C2. Implement MockedPromptProcessor reading assets/prompts.json, case-insensitive matching, trims whitespace.
  - Unknown prompt: return UiDescription(components=[]) with an error code or special Unknown marker; decision: return empty + status.
- C3. Add matching strategy: exact match first; optionally allow synonym table (stretch) but keep v1 exact to keep module deep/simple.
- C4. Tests (Unit):
  - PromptProcessorMockTest:
    - testExactMatchReturnsExpectedUiDescription
    - testCaseInsensitiveMatch
    - testUnknownPromptReturnsEmptyDescription
    - testJsonParsingForAllExamples

Definition of Done (Phase 1)
- MockedPromptProcessor passes unit tests and can produce UiDescription for 3–5 prompts.

---

## Phase 2 — State Management (Single Source of Truth)

Theme D: UI State Store
- D1. Define a UIState data holder (ViewModel or simple singleton) exposing:
  - getInitialState(): UiDescription
  - getCurrentState(): UiDescription
  - updateState(ui: UiDescription)
  - resetState()
- D2. Initial state: from a default UiDescription (e.g., title + neutral background). Keep independent of Compose.
- D3. Tests (Unit):
  - StateStoreTest:
    - testInitialStateEqualsCurrent
    - testUpdateStateReplacesCurrent
    - testResetRestoresInitial
    - testThreadSafetyIfUsingMutableStateFlow (optional; if using StateFlow, assert emission order)

Definition of Done (Phase 2)
- State store compiles; unit tests pass.

---

## Phase 3 — UI Rendering (Compose)

Theme E: Renderer (deep module, hides Compose details)
- E1. Implement Renderer composables reading UiDescription and mapping each UiComponent to Compose elements.
  - Background → Box modifier with background color
  - Title → Text with large style
  - Card → Card container with optional title text inside when provided
  - Button → Button composable with onClick invoking action handler callback
- E2. Define an ActionHandler interface:
  - interface ActionHandler { fun handle(action: String) }
  - Pass from MainActivity to Renderer; hides business logic from UI (information hiding).
- E3. Pure mapping function for testability:
  - Provide a UiToRenderModel mapper (pure) to support JVM unit tests without Compose runtime.
- E4. Tests:
  - UiToRenderModelTest (JVM): verifies mapping (e.g., Background("blue") → RenderBackground(Color.Blue)).
  - Compose UI Test (androidTest):
    - Given UiDescription with Title("My Profile"), assert text shows via ComposeTesting.
    - Given Background("blue"), verify semantics or content description, or test color via tag.

Definition of Done (Phase 3)
- Renderer compiles and UI tests for title & button interactions pass on device/emulator.

---

## Phase 4 — App Wiring and Interaction

Theme F: Main Screen & Prompt Bar
- F1. Create a main Composable screen:
  - Top area shows current UI via Renderer(uiState)
  - Bottom InputBar (TextField + Send button) to enter prompt
  - Reset button in TopAppBar or as a secondary action
- F2. Wire flow:
  - On send: PromptProcessor.processPrompt(input) → UiDescription → updateState
  - On reset: stateStore.resetState()
  - On unknown prompt: show Snackbar/Toast with suggestion list
- F3. Provide 3–5 prompt chips users can tap to auto-fill the input.
- F4. Tests:
  - Instrumentation: typing "Make background blue" updates background observable via semantics/Tag
  - Instrumentation: tapping Reset restores initial state

Definition of Done (Phase 4)
- Manual demo path works; two instrumentation tests pass.

---

## Phase 5 — Bonus: LLM-backed Processor (Optional)

Theme G: LLMPromptProcessor (behind feature flag)
- G1. Add interface implementation calling a provider (OpenAI or local) keyed by env var or local.properties.
- G2. Parse model output into UiDescription (strict JSON schema, reject unsafe content).
- G3. Fallback to MockedPromptProcessor on error.
- G4. Tests (Unit with fakes):
  - LLMPromptProcessorTest: test well-formed JSON → UiDescription; test error path → fallback.

Definition of Done (Phase 5)
- Compiles, unit tests pass; feature flag default OFF.

---

## Phase 6 — Polish and Observability

Theme H: UX & Telemetry
- H1. Provide visible prompt suggestions (3–5 examples).
- H2. Add basic logging around prompt → result mapping with a [DEBUG_LOG] prefix for testability.
- H3. Error states: unknown prompt, malformed assets, JSON errors.
- H4. README updates: how to run, supported prompts, screenshots.
- Tests: snapshot or simple assertions on log messages in unit tests.

Definition of Done (Phase 6)
- Docs updated; logs visible; basic error UX in place.

---

## Cross-cutting: Acceptance Criteria

- AC1. The app renders an initial UI (title + neutral background) at launch.
- AC2. User can enter at least 3 different prompts that cause visible UI changes:
  - "Make background blue" changes background color.
  - "Show title My Profile" changes title text.
  - "Add Save button" displays a button.
- AC3. A reset action restores the initial UI.
- AC4. MockedPromptProcessor reads from assets/prompts.json, not hardcoded in code.
- AC5. Code is modular (Renderer, PromptProcessor, StateStore) and follows the interfaces in architecture.md.
- AC6. Unit tests pass for models, prompt processing, and state store. At least 2 instrumentation tests pass for UI.

---

## Risks & Mitigations
- Compose color testing can be tricky: use testTags and semantics instead of raw color checks.
- Asset loading on Android: ensure AssetManager reads prompts.json on main thread? Prefer IO in background; for tests, load from resources with Robolectric or inject a resource loader interface.
- JSON evolution: keep models flexible; unknown components ignored safely.

---

## Execution Checklist (Traceability)

Files to be created/modified during implementation (after approval):
- app/src/main/java/com/example/cw/model/UiDescription.kt
- app/src/main/java/com/example/cw/prompt/PromptProcessor.kt
- app/src/main/java/com/example/cw/prompt/MockedPromptProcessor.kt
- app/src/main/java/com/example/cw/state/UIStateStore.kt
- app/src/main/java/com/example/cw/render/Renderer.kt
- app/src/main/java/com/example/cw/MainScreen.kt
- app/src/main/assets/prompts.json
- Tests (unit): app/src/test/java/com/example/cw/*
- Tests (androidTest): app/src/androidTest/java/com/example/cw/*
- README/architecture references as needed.

---

## Go/No-Go Checkpoint

This plan is ready. Please review and give the go-ahead to start Phase 0. On approval, I will execute by phases, keeping changes minimal, running tests after each phase, and reporting progress.
