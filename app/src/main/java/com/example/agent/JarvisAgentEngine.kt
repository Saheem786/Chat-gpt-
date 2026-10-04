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

            onUpdate("Analyzing and planning next steps...")
            val action = commandParser.parseCommand(userGoal, screenLayoutText)
            Log.d(TAG, "Planned action step: $action")
            delay(500)

            onUpdate("Executing screen operation: ${action.actionName}...")
            
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
                onUpdate("Verifying screen action results...")
                
                val expectedTarget = action.arguments["text"]?.toString() ?: action.arguments["direction"]?.toString()
                val verified = resultVerifier.verifyActionSucceeded(action.actionName, expectedTarget)
                
                if (verified) {
                    success = true
                    onUpdate("Screen action executed and verified successfully!")
                    Log.i(TAG, "Screen action step succeeded and verified.")
                } else {
                    retryCount++
                    onUpdate("Verification failed. Retrying screen analysis (Attempt $retryCount/3)...")
                    Log.w(TAG, "Action verification failed. Retrying loop step.")
                    delay(800)
                }
            } else {
                retryCount++
                onUpdate("Execution failed. Retrying (Attempt $retryCount/3)...")
                Log.e(TAG, "Action execution failed. Retrying loop.")
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
