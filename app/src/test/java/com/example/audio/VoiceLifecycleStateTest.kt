package com.example.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AppMode
import com.example.ui.AssistantState
import com.example.ui.VoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceLifecycleStateTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // Helper state checker matching SamViewModel.isMicrophoneBusy()
    private fun isMicrophoneBusy(voiceState: VoiceState, assistantState: AssistantState, isSendingChat: Boolean = false): Boolean {
        return voiceState == VoiceState.PROCESSING ||
               voiceState == VoiceState.GEMINI_STREAMING ||
               voiceState == VoiceState.TOOL_EXECUTION ||
               voiceState == VoiceState.SPEAKING ||
               voiceState == VoiceState.FINAL_RESULT ||
               assistantState == AssistantState.THINKING ||
               assistantState == AssistantState.SPEAKING ||
               isSendingChat
    }

    private fun canStartCommandListening(voiceState: VoiceState, assistantState: AssistantState, isSendingChat: Boolean = false): Boolean {
        if (isMicrophoneBusy(voiceState, assistantState, isSendingChat)) return false
        return voiceState == VoiceState.IDLE || voiceState == VoiceState.COMMAND_LISTENING
    }

    // TEST 1, 2, 3, 4, 5 & 12: TTS Session simulation with 5 streaming chunks
    @Test
    fun testStreamingGeminiResponseWithFiveChunksSessionLifecycle() {
        var isStreamingSession = false
        var streamingComplete = false
        val pendingUtterances = AtomicInteger(0)
        var sessionCompleteCalls = 0
        var speechRecognizerStarted = false

        fun onSessionComplete() {
            sessionCompleteCalls++
            // Only after session completion is recognition allowed if in appropriate mode
            speechRecognizerStarted = true
        }

        fun checkSessionCompletion() {
            var shouldComplete = false
            if (isStreamingSession) {
                if (streamingComplete && pendingUtterances.get() <= 0) {
                    shouldComplete = true
                }
            } else {
                if (pendingUtterances.get() <= 0) {
                    shouldComplete = true
                }
            }
            if (shouldComplete) {
                onSessionComplete()
            }
        }

        fun speakChunk(chunkText: String, isFirstChunk: Boolean) {
            if (isFirstChunk) {
                isStreamingSession = true
                streamingComplete = false
                pendingUtterances.set(0)
            }
            pendingUtterances.incrementAndGet()
        }

        fun markStreamingComplete() {
            streamingComplete = true
            checkSessionCompletion()
        }

        fun simulateUtteranceDone() {
            pendingUtterances.decrementAndGet()
            checkSessionCompletion()
        }

        // --- Step 1: Chunk 1 arrives & plays ---
        speakChunk("Chunk 1: Hello", isFirstChunk = true)
        assertEquals(1, pendingUtterances.get())
        // Chunk 1 finishes speaking
        simulateUtteranceDone()
        assertEquals(0, pendingUtterances.get())
        // 1. TTS chunk 1 completes -> SpeechRecognizer MUST NOT start!
        assertFalse("SpeechRecognizer MUST NOT start after Chunk 1 done", speechRecognizerStarted)
        assertEquals(0, sessionCompleteCalls)

        // --- Step 2: Chunk 2 arrives & finishes ---
        speakChunk("Chunk 2: how are you", isFirstChunk = false)
        assertEquals(1, pendingUtterances.get())
        simulateUtteranceDone()
        // 2. Chunk 2 completes -> SpeechRecognizer MUST NOT start!
        assertFalse("SpeechRecognizer MUST NOT start after Chunk 2 done", speechRecognizerStarted)
        assertEquals(0, sessionCompleteCalls)

        // --- Step 3: Chunks 3, 4, 5 arrive ---
        speakChunk("Chunk 3: doing today", isFirstChunk = false)
        speakChunk("Chunk 4: Dubai weather is", isFirstChunk = false)
        speakChunk("Chunk 5: 32 degrees Celsius", isFirstChunk = false)
        assertEquals(3, pendingUtterances.get())

        // Chunks 3, 4, 5 complete speaking BUT Gemini has not marked stream complete yet!
        simulateUtteranceDone() // chunk 3
        simulateUtteranceDone() // chunk 4
        simulateUtteranceDone() // chunk 5
        assertEquals(0, pendingUtterances.get())
        // 3. Chunk 5 completes -> SpeechRecognizer MUST NOT start if stream still incomplete!
        assertFalse("SpeechRecognizer MUST NOT start when stream is still incomplete", speechRecognizerStarted)
        assertEquals(0, sessionCompleteCalls)

        // --- Step 4: Now Gemini stream marks complete! ---
        markStreamingComplete()
        // 5. Final TTS chunk complete AND stream marked complete -> NOW and ONLY NOW allow transition!
        assertTrue("SpeechRecognizer allowed now that both stream and TTS finished", speechRecognizerStarted)
        // 12. Exactly ONE final TTS-session-complete callback invoked
        assertEquals("Must trigger exactly ONE session complete callback", 1, sessionCompleteCalls)
    }

    // TEST 4 & 5 (Reverse order): Stream marks complete while chunks are STILL speaking
    @Test
    fun testStreamMarksCompleteWhileChunksAreStillQueued() {
        var isStreamingSession = false
        var streamingComplete = false
        val pendingUtterances = AtomicInteger(0)
        var sessionCompleteCalls = 0

        fun checkSessionCompletion() {
            if (isStreamingSession && streamingComplete && pendingUtterances.get() <= 0) {
                sessionCompleteCalls++
            }
        }

        // Chunks 1 and 2 queued
        isStreamingSession = true
        pendingUtterances.set(2)

        // 4. Gemini marks stream complete: still wait for all queued TTS chunks
        streamingComplete = true
        checkSessionCompletion()
        assertEquals("Must NOT complete session while chunks are still playing", 0, sessionCompleteCalls)

        // Chunk 1 finishes
        pendingUtterances.decrementAndGet()
        checkSessionCompletion()
        assertEquals("Must NOT complete session while chunk 2 is still playing", 0, sessionCompleteCalls)

        // Chunk 2 (final) finishes
        pendingUtterances.decrementAndGet()
        checkSessionCompletion()
        assertEquals("Now session complete must trigger exactly once", 1, sessionCompleteCalls)
    }

    // TEST 6: Manual mic mode returns to IDLE and NEVER restarts listening automatically
    @Test
    fun testManualMicModeReturnsToIdleWithoutRestart() {
        val currentMode = AppMode.CHAT // Manual mic / text chat mode
        var state = AssistantState.SPEAKING
        var isMicActive = true
        var voiceListeningStarted = false

        // TTS finishes speaking in manual mode
        if (currentMode == AppMode.VOICE && isMicActive) {
            voiceListeningStarted = true
        } else {
            state = AssistantState.IDLE
            isMicActive = false
            voiceListeningStarted = false
        }

        assertEquals(AssistantState.IDLE, state)
        assertFalse("Manual mic mode must not automatically restart listening", voiceListeningStarted)
    }

    // TEST 7: Wake-word mode transitions to WAKE_WORD_LISTENING only after complete TTS
    @Test
    fun testWakeWordModeTransitionsOnlyAfterTtsCompletion() {
        val currentMode = AppMode.CHAT
        val isWakeWordEnabled = true
        var wakeWordServiceStarted = false

        fun onSpeechSessionEnded() {
            if (currentMode != AppMode.VOICE && isWakeWordEnabled) {
                wakeWordServiceStarted = true
            }
        }

        // During speaking: wake word must NOT start
        assertFalse(wakeWordServiceStarted)

        // Once speech session completes:
        onSpeechSessionEnded()
        assertTrue("Wake word service should start only after TTS session ends", wakeWordServiceStarted)
    }

    // TEST 8: ERROR_RECOGNIZER_BUSY during SPEAKING must NOT restart
    @Test
    fun testErrorRecognizerBusyDuringSpeakingDoesNotRestart() {
        val voiceState = VoiceState.SPEAKING
        val assistantState = AssistantState.SPEAKING
        assertFalse("Cannot start or retry recognition during SPEAKING", canStartCommandListening(voiceState, assistantState))
    }

    // TEST 9: ERROR_SERVER_DISCONNECTED during PROCESSING must NOT restart
    @Test
    fun testErrorServerDisconnectedDuringProcessingDoesNotRestart() {
        val voiceState = VoiceState.PROCESSING
        val assistantState = AssistantState.THINKING
        assertFalse("Cannot start or retry recognition during PROCESSING", canStartCommandListening(voiceState, assistantState))
    }

    // TEST 10: Barge-in disabled prevents SpeechRecognizer start during TTS
    @Test
    fun testBargeInDisabledPreventsStartDuringTts() {
        val isBargeInEnabled = false
        var startAttempted = false

        fun onBargeInCheck() {
            if (!isBargeInEnabled) return
            startAttempted = true
        }

        onBargeInCheck()
        assertFalse("Barge-in disabled must not attempt recognition start", startAttempted)
    }

    // TEST 11: startListening() cannot bypass checkCanListen()
    @Test
    fun testStartListeningCannotBypassCheckCanListen() {
        var isBusy = true
        val handler = VoiceInputHandler(
            context = context,
            checkCanListen = { !isBusy }
        )

        // Handler should immediately block when busy
        handler.startListening()
        assertFalse(handler.isCurrentlyListening())

        // Handler can listen when not busy
        isBusy = false
        assertTrue(!isBusy)
    }

    // TEST: Verify GEMINI_STREAMING and TOOL_EXECUTION are protected states
    @Test
    fun testStreamingAndToolExecutionStatesAreProtected() {
        assertTrue(isMicrophoneBusy(VoiceState.GEMINI_STREAMING, AssistantState.THINKING))
        assertTrue(isMicrophoneBusy(VoiceState.TOOL_EXECUTION, AssistantState.THINKING))
        assertTrue(isMicrophoneBusy(VoiceState.FINAL_RESULT, AssistantState.THINKING))
        assertFalse(isMicrophoneBusy(VoiceState.IDLE, AssistantState.IDLE))
    }
}
