package com.example.system

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

sealed class VerificationResult {
    object Success : VerificationResult()
    data class Failure(val reason: String) : VerificationResult()
    object PermissionRequired : VerificationResult()
}

class ActionResultVerifier {
    companion object {
        private const val TAG = "ActionResultVerifier"
    }

    fun verifyActionSucceeded(actionName: String, expectedTarget: String?): VerificationResult {
        Log.i(TAG, "Verifying action '' success for target: ")
        
        val service = SamScreenContextService.getInstance()
        if (service == null) {
            Log.e(TAG, "Accessibility Service is not active. Permission required!")
            return VerificationResult.PermissionRequired
        }
        val rootNode = service.rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "Root window node is null! Accessibility context inaccessible.")
            return VerificationResult.PermissionRequired
        }
        
        return when (actionName.lowercase()) {
            "click" -> {
                if (expectedTarget != null && expectedTarget.isNotBlank()) {
                    val nodes = findNodesByText(rootNode, expectedTarget)
                    if (nodes.isNotEmpty()) {
                        VerificationResult.Success
                    } else {
                        VerificationResult.Failure("Target text '' was not found on screen after click.")
                    }
                } else {
                    VerificationResult.Success
                }
            }
            "input_text" -> {
                if (expectedTarget != null && expectedTarget.isNotBlank()) {
                    val nodes = findNodesByText(rootNode, expectedTarget)
                    if (nodes.isNotEmpty()) {
                        VerificationResult.Success
                    } else {
                        VerificationResult.Failure("Typed text '' was not found in any text elements on screen.")
                    }
                } else {
                    VerificationResult.Failure("No target text provided to verify typing outcome.")
                }
            }
            "scroll" -> {
                VerificationResult.Success
            }
            else -> {
                VerificationResult.Failure("Unknown action execution: ")
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
