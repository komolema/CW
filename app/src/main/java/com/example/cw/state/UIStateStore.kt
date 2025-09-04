package com.example.cw.state

import com.example.cw.model.UiComponent
import com.example.cw.model.UiDescription

/**
 * Phase 2 — State Management (Single Source of Truth)
 *
 * A minimal, in-memory state manager that holds an initial UiDescription and a mutable
 * current UiDescription. Methods are thread-safe via simple synchronization.
 */
interface UiStateManager {
    fun getInitialState(): UiDescription
    fun getCurrentState(): UiDescription
    fun updateState(newUi: UiDescription)
    fun resetState()
}

class UiStateManagerImpl(
    private val initial: UiDescription = DefaultUiState.initial()
) : UiStateManager {

    @Volatile
    private var current: UiDescription = initial

    override fun getInitialState(): UiDescription = initial

    override fun getCurrentState(): UiDescription = current

    override fun updateState(newUi: UiDescription) {
        synchronized(this) {
            current = newUi
        }
    }

    override fun resetState() {
        synchronized(this) {
            current = initial
        }
    }
}

/**
 * Provides a default initial UI that is independent of Compose.
 */
object DefaultUiState {
    fun initial(): UiDescription = UiDescription(
        components = listOf(
            UiComponent(type = "background", color = "blue")
        )
    )
}
