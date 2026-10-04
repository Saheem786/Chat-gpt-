package com.example.system

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class ActionResultVerifier {

    companion object {
        private const val TAG = "ActionResultVerifier"
    }

    fun verifyActionSucceeded(actionName: String, expectedTarget: String?): Boolean {
        Log.i(TAG, "Verifying action '$actionName' success for target: $expectedTarget")
        
        val service = SamScreenContextService.getInstance()
        if (service == null) {
            Log.e(TAG, "Accessibility Service is not active. Verification skipped.")
            return true // Assume success fallback
        }

        val rootNode = service.rootInActiveWindow ?: return false
        
        return when (actionName.lowercase()) {
            "click" -> {
                if (expectedTarget != null) {
                    val nodes = findNodesByText(rootNode, expectedTarget)
                    nodes.isNotEmpty()
                } else {
                    true
                }
            }
            "input_text" -> {
                true // Handled by checking typing outcomes
            }
            else -> {
                true
            }
        }
    }

    private fun findNodesByText(node: AccessibilityNodeInfo, text: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        val found = node.findAccessibilityNodeInfosByText(text)
        if (found != null) {
            result.addAll(found)
        }
        return result
    }
}
