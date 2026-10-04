package com.example.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JarvisAgentEngineTest {

    @Test
    fun testActionRegistryRegistration() {
        val action = com.example.device.command.ActionRegistry.getAction("click")
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
}
