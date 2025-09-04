package com.example.cw.model

data class UiDescription(
    val components: List<UiComponent> = emptyList()
)

data class UiComponent(
    val type: String,
    val text: String? = null,
    val color: String? = null,
    val properties: Map<String, String> = emptyMap(),
    val action: String? = null
)
