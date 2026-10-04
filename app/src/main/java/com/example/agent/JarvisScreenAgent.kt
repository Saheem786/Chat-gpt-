package com.example.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.system.SamScreenContextService

class JarvisScreenAgent {

    companion object {
        private const val TAG = "JarvisScreenAgent"
    }

    fun executeAction(action: ParsedScreenAction): Boolean {
        Log.i(TAG, "Executing action: '${action.actionName}' with args: ${action.arguments}")
        
        val service = SamScreenContextService.getInstance()
        if (service == null) {
            Log.e(TAG, "Accessibility Service is not active. Unable to execute screen action.")
            return false
        }

        return when (action.actionName.lowercase()) {
            "click" -> {
                val text = action.arguments["text"]?.toString() ?: ""
                val x = action.arguments["x"] as? Int
                val y = action.arguments["y"] as? Int

                if (x != null && y != null) {
                    clickCoordinates(service, x, y)
                } else if (text.isNotBlank()) {
                    clickNodeWithText(service, text)
                } else {
                    false
                }
            }
            "input_text" -> {
                val text = action.arguments["text"]?.toString() ?: ""
                inputTextToActiveNode(service, text)
            }
            "scroll" -> {
                val direction = action.arguments["direction"]?.toString() ?: "DOWN"
                scrollScreen(service, direction)
            }
            else -> {
                false
            }
        }
    }

    private fun clickNodeWithText(service: SamScreenContextService, text: String): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        if (!nodes.isNullOrEmpty()) {
            for (node in nodes) {
                if (node.isClickable) {
                    val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) {
                        Log.i(TAG, "Successfully clicked node with text: '$text'")
                        return true
                    }
                } else {
                    // Try parent nodes
                    var parent = node.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            if (clicked) {
                                Log.i(TAG, "Successfully clicked parent node of text: '$text'")
                                return true
                            }
                        }
                        parent = parent.parent
                    }
                }
            }
        }
        return false
    }

    private fun clickCoordinates(service: SamScreenContextService, x: Int, y: Int): Boolean {
        Log.i(TAG, "Performing coordinates click at ($x, $y)")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val clickPath = Path().apply {
                moveTo(x.toFloat(), y.toFloat())
            }
            val gesture = GestureDescription.Builder().apply {
                addStroke(GestureDescription.StrokeDescription(clickPath, 0, 100))
            }.build()
            
            return service.dispatchGesture(gesture, null, null)
        }
        return false
    }

    private fun inputTextToActiveNode(service: SamScreenContextService, text: String): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val focusedNode = findFocusedNode(rootNode)
        if (focusedNode != null) {
            val arguments = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val typed = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (typed) {
                Log.i(TAG, "Successfully typed text into active focused element.")
                return true
            }
        }
        return false
    }

    private fun findFocusedNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFocusedNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun scrollScreen(service: SamScreenContextService, direction: String): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(rootNode)
        if (scrollableNode != null) {
            val action = if (direction.uppercase() == "UP") {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            return scrollableNode.performAction(action)
        }
        return false
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }
}
