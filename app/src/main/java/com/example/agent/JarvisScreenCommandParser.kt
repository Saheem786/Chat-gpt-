package com.example.agent

import android.util.Log

data class ParsedScreenAction(
    val actionName: String, // "click", "input_text", "scroll", "navigate"
    val arguments: Map<String, Any?>
)

class JarvisScreenCommandParser {

    companion object {
        private const val TAG = "JarvisScreenCommandParser"
    }

    fun parseCommand(userInput: String, screenLayoutText: String): ParsedScreenAction {
        Log.i(TAG, "Parsing user input: '$userInput'")
        val lowerInput = userInput.lowercase()

        return when {
            lowerInput.contains("click") || lowerInput.contains("tap") -> {
                val targetText = extractTargetText(userInput, listOf("click on", "click", "tap on", "tap"))
                ParsedScreenAction(
                    actionName = "click",
                    arguments = mapOf("text" to targetText)
                )
            }
            lowerInput.contains("type") || lowerInput.contains("write") || lowerInput.contains("enter") -> {
                val targetText = extractTextToType(userInput)
                ParsedScreenAction(
                    actionName = "input_text",
                    arguments = mapOf("text" to targetText)
                )
            }
            lowerInput.contains("scroll") || lowerInput.contains("swipe") -> {
                val direction = when {
                    lowerInput.contains("up") -> "UP"
                    lowerInput.contains("left") -> "LEFT"
                    lowerInput.contains("right") -> "RIGHT"
                    else -> "DOWN"
                }
                ParsedScreenAction(
                    actionName = "scroll",
                    arguments = mapOf("direction" to direction)
                )
            }
            lowerInput.contains("go back") || lowerInput.contains("back") -> {
                ParsedScreenAction(
                    actionName = "navigate",
                    arguments = mapOf("destination" to "BACK")
                )
            }
            lowerInput.contains("go home") || lowerInput.contains("home") -> {
                ParsedScreenAction(
                    actionName = "navigate",
                    arguments = mapOf("destination" to "HOME")
                )
            }
            else -> {
                // Default fallback: search click
                ParsedScreenAction(
                    actionName = "click",
                    arguments = mapOf("text" to userInput)
                )
            }
        }
    }

    private fun extractTargetText(input: String, prefixes: List<String>): String {
        var clean = input
        for (prefix in prefixes) {
            if (clean.lowercase().startsWith(prefix)) {
                clean = clean.substring(prefix.length).trim()
            }
        }
        return clean
    }

    private fun extractTextToType(input: String): String {
        val regex = Regex("(?i)(?:type|write|enter)\\s+[\"']?(.*?)[\"']?\\s+(?:in|into|on)\\s+(.*)")
        val match = regex.find(input)
        return if (match != null) {
            match.groupValues[1].trim()
        } else {
            input.replace(Regex("(?i)^(type|write|enter)\\s+"), "").trim()
        }
    }
}
