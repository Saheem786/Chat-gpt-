package com.example.agent

import android.content.Context
import android.util.Log
import com.example.system.ActionResultVerifier
import com.example.system.NavigationManager
import com.example.system.SamScreenContextService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class JarvisAgentEngine(private val context: Context) {

    companion object {
        private const val TAG = "JarvisAgentEngine"
        private const val MAX_RETRIES = 3
    }

    private val screenAgent = JarvisScreenAgent()
    private val commandParser = JarvisScreenCommandParser()
    private val resultVerifier = ActionResultVerifier()
    private val navigationManager = NavigationManager(context)

    suspend fun runAgentLoop(userGoal: String, onUpdate: (String) -> Unit): Boolean = withContext(Dispatchers.Default) {
        Log.i(TAG, "Starting screen agent loop for goal: '$userGoal'")
        onUpdate("Scanning your screen layout context...")
        delay(600)

        var retryCount = 0
        var success = false

        while (retryCount < MAX_RETRIES && !success) {
            val service = SamScreenContextService.getInstance()
            if (service == null) {
                Log.e(TAG, "Accessibility Service not running.")
                onUpdate("Error: Please enable 'JARVIS Screen Reader' in accessibility settings.")
                return@withContext false
            }

            val rootNode = service.rootInActiveWindow
            val screenLayoutText = rootNode?.let { node ->
                val sb = StringBuilder()
                extractLayoutDetails(node, sb)
                sb.toString()
            } ?: ""

            // 1. PLAN Phase
            onUpdate("PLAN Phase: Analyzing screen layout and parsing goal...")
            val action = commandParser.parseCommand(userGoal, screenLayoutText)
            Log.d(TAG, "Planned action step: $action")
            onUpdate("PLAN: Formulated step [${action.actionName}] targeting elements matching [${action.arguments["text"] ?: action.arguments["direction"] ?: ""}]")
            delay(600)

            // 2. ACT Phase (ActionRegistry is our gatekeeper)
            onUpdate("ACT Phase: Passing action through ActionRegistry execution gate...")
            val executionCheck = com.example.device.command.ActionRegistry.canExecuteAction(action.actionName, action.arguments)
            when (executionCheck) {
                is com.example.device.command.ExecutionResult.Denied -> {
                    onUpdate("ACT blocked: Security Policy DENIED execution of action '${action.actionName}'!")
                    Log.e(TAG, "ACT blocked by security policy.")
                    return@withContext false
                }
                is com.example.device.command.ExecutionResult.Restricted -> {
                    onUpdate("ACT blocked: RESTRICTED/UNKNOWN action: ${(executionCheck as com.example.device.command.ExecutionResult.Restricted).reason}")
                    Log.e(TAG, "ACT blocked: Restricted/Unknown action.")
                    return@withContext false
                }
                com.example.device.command.ExecutionResult.Allowed -> {
                    onUpdate("ACT Phase: Action verified and allowed by Security Policy.")
                }
            }

            onUpdate("ACT Phase: Executing screen operation: ${action.actionName}...")
            val executed = if (action.actionName.lowercase() == "navigate") {
                val dest = action.arguments["destination"]?.toString() ?: "HOME"
                if (dest == "BACK") {
                    navigationManager.navigateBack()
                } else {
                    navigationManager.navigateHome()
                }
            } else {
                screenAgent.executeAction(action)
            }

            if (executed) {
                delay(800) // Allow screen transition/update layout
                
                // 3. VERIFY Phase
                onUpdate("VERIFY Phase: Inspecting actual screen state changes...")
                val expectedTarget = action.arguments["text"]?.toString() ?: action.arguments["direction"]?.toString()
                val verification = resultVerifier.verifyActionSucceeded(action.actionName, expectedTarget)
                
                // 4. REFLECT Phase
                onUpdate("REFLECT Phase: Evaluating action execution outcome...")
                when (verification) {
                    is com.example.system.VerificationResult.Success -> {
                        success = true
                        onUpdate("REFLECT: Action verified successfully! Goal achieved.")
                        Log.i(TAG, "Screen action step succeeded and verified.")
                    }
                    is com.example.system.VerificationResult.PermissionRequired -> {
                        onUpdate("REFLECT: Verification failed: Accessibility service died or permissions required.")
                        Log.e(TAG, "Accessibility service context missing during verification.")
                        return@withContext false
                    }
                    is com.example.system.VerificationResult.Failure -> {
                        retryCount++
                        onUpdate("REFLECT: Action verification failed (Attempt $retryCount/3): ${(verification as com.example.system.VerificationResult.Failure).reason}")
                        Log.w(TAG, "Action verification failed. Retrying.")
                        delay(800)
                    }
                }
            } else {
                retryCount++
                onUpdate("REFLECT: Action execution failed. Retrying (Attempt $retryCount/3)...")
                Log.e(TAG, "Action execution failed.")
                delay(800)
            }
        }
        return@withContext success
    }

    private fun extractLayoutDetails(node: android.view.accessibility.AccessibilityNodeInfo, sb: StringBuilder) {
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        if (text.isNotBlank() || contentDesc.isNotBlank()) {
            sb.append("Element: text='$text', desc='$contentDesc'\n")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractLayoutDetails(child, sb)
        }
    }
}
