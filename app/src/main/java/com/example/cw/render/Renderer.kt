package com.example.cw.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cw.model.UiComponent
import com.example.cw.model.UiDescription

/**
 * Action handler for button actions. Business logic is hidden from the renderer.
 */
fun interface ActionHandler {
    fun handle(action: String)
}

/** Pure render models (no Compose dependencies except optional mapping later) */
sealed interface RenderComponent

data class RenderBackground(val color: ColorName) : RenderComponent

data class RenderTitle(val text: String) : RenderComponent

data class RenderCard(val title: String?, val background: ColorName?) : RenderComponent

data class RenderButton(val text: String, val action: String?) : RenderComponent

/** A simple color name enum to keep mapping pure */
enum class ColorName { BLUE, GREEN, WHITE, RED, BLACK, PURPLE, YELLOW, UNKNOWN }

/**
 * Pure mapper: UiDescription -> List<RenderComponent>
 */
object UiToRenderModelMapper {
    fun map(ui: UiDescription): List<RenderComponent> = ui.components.mapNotNull { mapComponent(it) }

    private fun mapComponent(c: UiComponent): RenderComponent? = when (c.type.lowercase()) {
        "background" -> RenderBackground(parseColorName(c.color))
        "title" -> c.text?.let { RenderTitle(it) }
        "card" -> RenderCard(
            title = c.properties["title"],
            background = parseColorName(c.properties["backgroundColor"])
        )
        "button" -> RenderButton(text = c.text ?: "", action = c.action)
        else -> null
    }

    fun parseColorName(name: String?): ColorName = when (name?.trim()?.lowercase()) {
        "blue" -> ColorName.BLUE
        "green" -> ColorName.GREEN
        "white" -> ColorName.WHITE
        "red" -> ColorName.RED
        "black" -> ColorName.BLACK
        "purple" -> ColorName.PURPLE
        "yellow" -> ColorName.YELLOW
        null, "" -> ColorName.UNKNOWN
        else -> ColorName.UNKNOWN
    }
}

/** Map ColorName to Compose Color */
private fun colorNameToColor(colorName: ColorName): Color = when (colorName) {
    ColorName.BLUE -> Color(0xFF2196F3)
    ColorName.GREEN -> Color(0xFF4CAF50)
    ColorName.WHITE -> Color.White
    ColorName.RED -> Color(0xFFF44336)
    ColorName.BLACK -> Color.Black
    ColorName.PURPLE -> Color(0xFF9C27B0)
    ColorName.YELLOW -> Color(0xFFFFEB3B)
    ColorName.UNKNOWN -> Color(0xFFF0F0F0)
}

/**
 * Compose renderer that implements the deep module contract.
 */
object Renderer : RendererContract {
    @Composable
    override fun renderUI(uiDescription: UiDescription, actionHandler: ActionHandler) {
        val renderComponents = UiToRenderModelMapper.map(uiDescription)
        // Determine background (last background wins)
        val bg = renderComponents.lastOrNull { it is RenderBackground } as? RenderBackground
        val bgColor = bg?.color?.let { colorNameToColor(it) } ?: MaterialTheme.colorScheme.background

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .testTag("backgroundBox")
                .semantics { contentDescription = "bg:${(bg?.color ?: ColorName.UNKNOWN).name}" }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val isColoredBg = bg?.color == ColorName.BLUE || bg?.color == ColorName.GREEN
                renderComponents.forEach { rc ->
                    when (rc) {
                        is RenderTitle -> Text(
                            text = rc.text,
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (isColoredBg) Color.White else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.testTag("titleText")
                        )
                        is RenderCard -> Card(
                            colors = CardDefaults.cardColors(
                                containerColor = rc.background?.let { colorNameToColor(it) }
                                    ?: MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .testTag("card")
                        ) {
                            rc.title?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        is RenderButton -> Button(
                            onClick = { rc.action?.let { actionHandler.handle(it) } },
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .testTag("actionButton")
                        ) {
                            Text(text = rc.text)
                        }
                        is RenderBackground -> Unit // already applied via bg
                    }
                }
            }
        }
    }
}
