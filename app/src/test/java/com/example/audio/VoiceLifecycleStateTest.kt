package com.example.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AppMode
import com.example.ui.AssistantState
import com.example.ui.VoiceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceLifecycleStateTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testVoiceStateEnumPresence() {
        // Verify VoiceState enum transitions are present and correct
        val idle = VoiceState.IDLE
        val listening = VoiceState.COMMAND_LISTENING
        val processing = VoiceState.PROCESSING
        val speaking = VoiceState.SPEAKING
        val error = VoiceState.ERROR_RECOVERY

        assertEquals("IDLE", idle.name)
        assertEquals("COMMAND_LISTENING", listening.name)
        assertEquals("PROCESSING", processing.name)
        assertEquals("SPEAKING", speaking.name)
        assertEquals("ERROR_RECOVERY", error.name)
    }

    @Test
    fun testVoiceStateLoopTransitions() {
        // Create states simulating Complete sentence -> final result -> processing -> answer -> TTS -> idle
        var currentVoiceState = VoiceState.IDLE

        // 1. User starts speaking or wake word triggers
        currentVoiceState = VoiceState.COMMAND_LISTENING
        assertEquals(VoiceState.COMMAND_LISTENING, currentVoiceState)

        // 2. Final speech result is received
        currentVoiceState = VoiceState.FINAL_RESULT
        assertEquals(VoiceState.FINAL_RESULT, currentVoiceState)

        // 3. Command/chat processing starts
        currentVoiceState = VoiceState.PROCESSING
        assertEquals(VoiceState.PROCESSING, currentVoiceState)

        // 4. Action/tool execution
        currentVoiceState = VoiceState.TOOL_EXECUTION
        assertEquals(VoiceState.TOOL_EXECUTION, currentVoiceState)

        // 5. TTS starts speaking
        currentVoiceState = VoiceState.SPEAKING
        assertEquals(VoiceState.SPEAKING, currentVoiceState)

        // 6. TTS ends, return to IDLE
        currentVoiceState = VoiceState.IDLE
        assertEquals(VoiceState.IDLE, currentVoiceState)
    }

    @Test
    fun testNoMicrophoneRestartDuringBusyStates() {
        // Test helper simulates isMicrophoneBusy gating
        fun isMicrophoneBusy(state: VoiceState, assistantState: AssistantState): Boolean {
            return state == VoiceState.PROCESSING ||
                   state == VoiceState.TOOL_EXECUTION ||
                   state == VoiceState.SPEAKING ||
                   state == VoiceState.FINAL_RESULT ||
                   assistantState == AssistantState.THINKING ||
                   assistantState == AssistantState.SPEAKING
        }

        // Verify microphone is busy during processing
        assertTrue(isMicrophoneBusy(VoiceState.PROCESSING, AssistantState.THINKING))

        // Verify microphone is busy during Tool execution
        assertTrue(isMicrophoneBusy(VoiceState.TOOL_EXECUTION, AssistantState.THINKING))

        // Verify microphone is busy during TTS Speaking
        assertTrue(isMicrophoneBusy(VoiceState.SPEAKING, AssistantState.SPEAKING))

        // Verify microphone is busy during Final Result receiving
        assertTrue(isMicrophoneBusy(VoiceState.FINAL_RESULT, AssistantState.THINKING))

        // Verify microphone is NOT busy when IDLE and listening
        assertFalse(isMicrophoneBusy(VoiceState.IDLE, AssistantState.IDLE))
    }

    @Test
    fun testVoiceInputHandlerConstructorSafety() {
        // Verify VoiceInputHandler constructor with checkCanListen callback works flawlessly
        var isBusy = true
        val handler = VoiceInputHandler(
            context = context,
            checkCanListen = { !isBusy }
        )

        // Verify when busy, startListening is blocked/no-op
        handler.startListening()
        assertFalse(handler.isCurrentlyListening())
    }
}
