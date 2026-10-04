package com.example.device.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ActionSecurityPolicyTest {

    @Test
    fun testSafeActions() {
        val policy = ActionSecurityPolicy()
        val result = policy.checkAction("toggle_wifi", emptyMap())
        assertEquals(SecurityLevel.SAFE, result.level)
        assertFalse(result.requiresConfirmation)
    }

    @Test
    fun testSmsConfirmationRequired() {
        val policy = ActionSecurityPolicy()
        val args = mapOf("recipient" to "Mom", "message" to "On my way!")
        val result = policy.checkAction("send_sms", args)
        assertEquals(SecurityLevel.CONFIRMATION_REQUIRED, result.level)
        assertTrue(result.requiresConfirmation)
        assertTrue(result.promptMessage?.contains("Mom") == true)
        assertTrue(result.promptMessage?.contains("On my way!") == true)
    }

    @Test
    fun testCallConfirmationRequired() {
        val policy = ActionSecurityPolicy()
        val args = mapOf("phoneNumber" to "+123456789")
        val result = policy.checkAction("make_call", args)
        assertEquals(SecurityLevel.CONFIRMATION_REQUIRED, result.level)
        assertTrue(result.requiresConfirmation)
        assertTrue(result.promptMessage?.contains("+123456789") == true)
    }
}
