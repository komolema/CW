package com.example.cw.render

import androidx.compose.runtime.Composable
import com.example.cw.model.UiDescription

/**
 * Rendering contract. Implementation will be provided in Phase 3 using Jetpack Compose.
 * Deep module: hides Compose details from the rest of the app.
 */
interface RendererContract {
    @Composable
    fun renderUI(uiDescription: UiDescription, actionHandler: ActionHandler)
}
