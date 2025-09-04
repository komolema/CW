package com.example.cw

import com.example.cw.model.UiComponent
import com.example.cw.model.UiDescription
import com.example.cw.state.DefaultUiState
import com.example.cw.state.UiStateManager
import com.example.cw.state.UiStateManagerImpl
import org.junit.Assert.assertEquals
import org.junit.Test

class StateStoreTest {

    @Test
    fun testInitialStateEqualsCurrent() {
        val manager: UiStateManager = UiStateManagerImpl()
        val initial = manager.getInitialState()
        val current = manager.getCurrentState()
        assertEquals(initial, current)
        assertEquals(DefaultUiState.initial(), initial)
    }

    @Test
    fun testUpdateStateReplacesCurrent() {
        val manager: UiStateManager = UiStateManagerImpl()
        val newUi = UiDescription(
            components = listOf(
                UiComponent(type = "title", text = "Changed"),
                UiComponent(type = "background", color = "blue")
            )
        )
        manager.updateState(newUi)
        assertEquals(newUi, manager.getCurrentState())
    }

    @Test
    fun testResetRestoresInitial() {
        val manager: UiStateManager = UiStateManagerImpl()
        val initial = manager.getInitialState()
        val modified = UiDescription(
            components = listOf(
                UiComponent(type = "title", text = "Temp"),
                UiComponent(type = "background", color = "green")
            )
        )
        manager.updateState(modified)
        manager.resetState()
        assertEquals(initial, manager.getCurrentState())
    }
}
