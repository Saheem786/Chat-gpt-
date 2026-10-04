package com.example.agent

import com.example.device.command.ActionRegistry
import com.example.device.command.ExecutionResult
import com.example.system.ActionResultVerifier
import com.example.system.VerificationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JarvisAgentEngineTest {

    @Test
    fun testActionRegistryRegistration() {
        val action = ActionRegistry.getAction("click")
        assertNotNull(action)
        assertEquals("click", action?.name)
    }

    @Test
    fun testCommandParserClick() {
        val parser = JarvisScreenCommandParser()
        val action = parser.parseCommand("click on next button", "")
        assertEquals("click", action.actionName)
        assertEquals("next button", action.arguments["text"])
    }

    @Test
    fun testCommandParserType() {
        val parser = JarvisScreenCommandParser()
        val action = parser.parseCommand("type Hello World in field", "")
        assertEquals("input_text", action.actionName)
        assertEquals("Hello World", action.arguments["text"])
    }

    @Test
    fun testActionRegistryExecutionGateAllowed() {
        // Safe registered action with safe arguments
        val result = ActionRegistry.canExecuteAction("click", mapOf("text" to "submit"))
        assertEquals(ExecutionResult.Allowed, result)
    }

    @Test
    fun testActionRegistryExecutionGateUnknownActionDenied() {
        // Unknown/unregistered action
        val result = ActionRegistry.canExecuteAction("dangerous_root_exploit", emptyMap())
        assertTrue(result is ExecutionResult.Restricted)
    }

    @Test
    fun testActionRegistryExecutionGateRiskyActionDenied() {
        // Registered action but containing risky keyword
        val result = ActionRegistry.canExecuteAction("click", mapOf("text" to "factory_reset"))
        assertTrue(result is ExecutionResult.Denied)
    }

    @Test
    fun testActionResultVerifierAccessibilityUnavailable() {
        // When accessibility service is not running, ActionResultVerifier must return PermissionRequired
        val verifier = ActionResultVerifier()
        val result = verifier.verifyActionSucceeded("click", "next")
        assertEquals(VerificationResult.PermissionRequired, result)
    }
}
