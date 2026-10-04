package com.example.device.command

import android.util.Log

data class ActionDefinition(
    val name: String,
    val description: String,
    val parameterSchema: Map<String, String>
)

object ActionRegistry {
    private const val TAG = "ActionRegistry"
    private val registry = mutableMapOf<String, ActionDefinition>()

    init {
        // Register default screen actions
        register(
            ActionDefinition(
                name = "click",
                description = "Clicks on a specific UI element on the screen using text or coordinates.",
                parameterSchema = mapOf("text" to "String", "x" to "Int", "y" to "String")
            )
        )
        register(
            ActionDefinition(
                name = "input_text",
                description = "Inputs text into an active editable field on the screen.",
                parameterSchema = mapOf("text" to "String", "elementId" to "String")
            )
        )
        register(
            ActionDefinition(
                name = "scroll",
                description = "Scrolls the screen in a specified direction (UP, DOWN, LEFT, RIGHT).",
                parameterSchema = mapOf("direction" to "String")
            )
        )
        register(
            ActionDefinition(
                name = "navigate",
                description = "Performs navigation (BACK, HOME, RECENTS).",
                parameterSchema = mapOf("destination" to "String")
            )
        )
    }

    fun register(definition: ActionDefinition) {
        registry[definition.name] = definition
        Log.d(TAG, "Registered action: ${definition.name}")
    }

    fun getAction(name: String): ActionDefinition? {
        return registry[name]
    }

    fun getAllActions(): List<ActionDefinition> {
        return registry.values.toList()
    }
}
