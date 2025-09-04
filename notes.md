# notes.md

## Phase 0 — Notes

Date: 2025-09-03 18:10 local

Summary
- Completed Phase 0 (Foundations): created domain models (UiDescription, UiComponent), defined PromptProcessor interface, and RendererContract (renderUI signature). Updated Gradle to include Kotlin serialization plugin and dependency to support JSON in later phases. Added initial unit tests for models and the strategy interface.

Decisions
- Models as plain Kotlin data classes for now (no @Serializable annotations) to avoid issues flagged by the tool about opt-in InternalSerializationApi on sealed/polymorphic usage. We will introduce serialization annotations (or manual parsing) in Phase 1 when we wire the mocked prompt processor and finalize the JSON schema. This keeps interfaces stable and complexity pulled into lower modules later, aligning with Ousterhout’s principles.
- PromptProcessor is a functional interface (fun interface) for simple injection and testing.
- Renderer is defined by a small interface (RendererContract) to hide Compose specifics from the rest of the app (deep module).

Build/Tooling
- Updated gradle/libs.versions.toml with kotlinx-serialization JSON dependency and plugin alias.
- Updated app/build.gradle.kts to apply kotlin-serialization plugin and add implementation(libs.kotlinx.serialization.json).

Tests
- Added UiModelTest covering:
  - Model instantiation and defaults.
  - PromptProcessor interface usage (returns empty UiDescription in Phase 0).
- JSON roundtrip tests are deferred to Phase 1 alongside the mocked processor, to avoid premature tight coupling and address the earlier annotation warnings.

Risks and Mitigations
- Serialization strategy: because we deferred annotations (due to InternalSerializationApi warnings by the analyzer), in Phase 1 we’ll either:
  - Use simple, explicit parsing of components (String type + nullable fields) with kotlinx.serialization; or
  - Introduce a small custom decoder for the flexible component list.
- Android test environment: instrumentation tests will be added in Phase 3–4; for now, JVM unit tests suffice.

Next Steps
- Run unit tests to verify Phase 0 compiles and tests pass.
- On your confirmation, proceed to Phase 1: MockedPromptProcessor, prompts.json, and JSON parsing tests.

## Phase 1 — Notes

Date: 2025-09-03 18:28 local

Summary
- Implemented MockedPromptProcessor with manual JSON parsing using kotlinx.serialization Json.parseToJsonElement to avoid serialization opt-ins.
- Matching is case-insensitive and trims whitespace; unknown prompts return an empty UiDescription (no components).
- Added app/src/main/assets/prompts.json with five examples: background blue, profile card, title text, save button, reset.
- Added MockedPromptProcessorTest with 4 tests covering exact match, case-insensitive match, unknown prompt behavior, and JSON parsing for all examples.

Decisions
- Kept domain models free of @Serializable to avoid InternalSerializationApi; parsing is done in the mocked processor layer via manual JSON traversal.
- Provided two factories for the mocked processor: fromJson (for JVM tests) and fromAssets (for runtime usage in the app when wiring happens in later phases).
- Reset prompt maps to an empty UI; state-store based reset will be added in Phase 2.

Verification
- Unit tests passed: 4/4.

Risks and Mitigations
- JSON schema evolution: manual parser tolerates unknown keys (Json { ignoreUnknownKeys = true }).
- Assets loading is Android-only; JVM tests rely on fromJson to avoid Android dependency.

Next Steps
- Phase 2: Implement State Management store (initial/current/reset) with unit tests.
- Phase 3: Renderer implementation in Compose.
