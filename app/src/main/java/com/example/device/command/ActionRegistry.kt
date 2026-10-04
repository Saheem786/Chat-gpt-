package com.example.device.command

import android.util.Log

data class ActionDefinition(
    val name: String,
    val description: String,
    val parameterSchema: Map<String, String>
)

sealed class ExecutionResult {
    object Allowed : ExecutionResult()
    data class Denied(val reason: String) : ExecutionResult()
    data class Restricted(val reason: String) : ExecutionResult()
}

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

    fun canExecuteAction(actionName: String, arguments: Map<String, Any?>): ExecutionResult {
        // 1. Gating Step: Check if the action is registered
        val definition = getAction(actionName)
        if (definition == null) {
            Log.e(TAG, "Blocked unregistered/unknown action: ''")
            return ExecutionResult.Restricted("Action '' is unknown/unregistered.")
        }

        // 2. Gating Step: Pass through ActionSecurityPolicy check
        val securityPolicy = ActionSecurityPolicy()
        val securityCheck = securityPolicy.checkAction(actionName, arguments)
        if (securityCheck.level == SecurityLevel.RESTRICTED) {
            Log.e(TAG, "Blocked restricted action: '' by security policy!")
            return ExecutionResult.Denied("Blocked restricted action: ''")
        }

        return ExecutionResult.Allowed
    }

    fun getAllActions(): List<ActionDefinition> {
        return registry.values.toList()
    }
}
