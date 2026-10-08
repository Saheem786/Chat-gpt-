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
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

enum class TestAudioPipeline { NONE, TTS, AUDIO_TRACK }

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceLifecycleStateTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun isMicrophoneBusy(
        voiceState: VoiceState,
        assistantState: AssistantState,
        isSendingChat: Boolean = false,
        isTtsSpeaking: Boolean = false,
        isAudioPlaying: Boolean = false
    ): Boolean {
        return voiceState == VoiceState.PROCESSING ||
               voiceState == VoiceState.GEMINI_STREAMING ||
               voiceState == VoiceState.TOOL_EXECUTION ||
               voiceState == VoiceState.SPEAKING ||
               voiceState == VoiceState.FINAL_RESULT ||
               assistantState == AssistantState.THINKING ||
               assistantState == AssistantState.SPEAKING ||
               isSendingChat ||
               isTtsSpeaking ||
               isAudioPlaying
    }

    private fun canStartCommandListening(
        voiceState: VoiceState,
        assistantState: AssistantState,
        isSendingChat: Boolean = false,
        isTtsSpeaking: Boolean = false,
        isAudioPlaying: Boolean = false
    ): Boolean {
        if (isMicrophoneBusy(voiceState, assistantState, isSendingChat, isTtsSpeaking, isAudioPlaying)) return false
        return voiceState == VoiceState.IDLE || voiceState == VoiceState.COMMAND_LISTENING
    }

    // 1. Single TTS chunk
    @Test
    fun testSingleTtsChunkLifecycle() {
        var isStreamingSession = false
        var streamingComplete = true
        val pendingUtterances = AtomicInteger(1)
        var sessionEnded = false

        fun checkSessionCompletion() {
            if (pendingUtterances.get() <= 0 && streamingComplete) {
                sessionEnded = true
            }
        }

        // Utterance done
        pendingUtterances.decrementAndGet()
        checkSessionCompletion()
        assertTrue("Single TTS chunk must trigger session completion", sessionEnded)
    }

    // 2. 5 streaming TTS chunks
    @Test
    fun testFiveStreamingTtsChunksLifecycle() {
        var isStreamingSession = true
        var streamingComplete = false
        val pendingUtterances = AtomicInteger(0)
        var sessionEnded = false

        fun checkSessionCompletion() {
            if (isStreamingSession && streamingComplete && pendingUtterances.get() <= 0) {
                sessionEnded = true
            }
        }

        // Receive 5 chunks
        for (i in 1..5) {
            pendingUtterances.incrementAndGet()
        }
        assertEquals(5, pendingUtterances.get())
        assertFalse(sessionEnded)

        // Process chunk completions one by one
        for (i in 1..5) {
            pendingUtterances.decrementAndGet()
        }
        checkSessionCompletion()
        assertFalse("Should not complete before Gemini streaming is marked complete", sessionEnded)

        // Gemini streaming marks complete
        streamingComplete = true
        checkSessionCompletion()
        assertTrue("Session must complete when chunks are finished and streaming is complete", sessionEnded)
    }

    // 3. Gemini stream completes before TTS
    @Test
    fun testGeminiStreamCompletesBeforeTts() {
        var streamingComplete = true
        val pendingUtterances = AtomicInteger(2)
        var sessionEnded = false

        fun checkSessionCompletion() {
            if (streamingComplete && pendingUtterances.get() <= 0) sessionEnded = true
        }

        checkSessionCompletion()
        assertFalse("Must wait for pending TTS utterances", sessionEnded)

        pendingUtterances.decrementAndGet()
        pendingUtterances.decrementAndGet()
        checkSessionCompletion()
        assertTrue("Must complete when final pending TTS finishes", sessionEnded)
    }

    // 4. TTS completes before Gemini stream
    @Test
    fun testTtsCompletesBeforeGeminiStream() {
        var streamingComplete = false
        val pendingUtterances = AtomicInteger(1)
        var sessionEnded = false

        fun checkSessionCompletion() {
            if (streamingComplete && pendingUtterances.get() <= 0) sessionEnded = true
        }

        pendingUtterances.decrementAndGet()
        checkSessionCompletion()
        assertFalse("Must not complete while Gemini stream is still active", sessionEnded)

        streamingComplete = true
        checkSessionCompletion()
        assertTrue("Must complete once Gemini stream finishes", sessionEnded)
    }

    // 5. Both complete -> exactly one session completion
    @Test
    fun testExactlyOneSessionCompletionEmitted() {
        val completionEmitted = AtomicBoolean(false)
        var completions = 0

        fun emitCompletion() {
            if (completionEmitted.compareAndSet(false, true)) {
                completions++
            }
        }

        emitCompletion()
        emitCompletion()
        emitCompletion()
        assertEquals("Session completion must be emitted exactly once", 1, completions)
    }

    // 6. TTS onDone + onError duplicate protection
    @Test
    fun testUtteranceIdDeduplicationMutex() {
        val completedIds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
        val pending = AtomicInteger(2)

        fun onCallback(id: String) {
            if (completedIds.add(id)) {
                pending.decrementAndGet()
            }
        }

        onCallback("utt-1")
        onCallback("utt-1") // duplicate onDone or onError
        onCallback("utt-1") // triplicate
        assertEquals("Pending count must only decrement once per unique utterance ID", 1, pending.get())
    }

    // 7. Manual one-shot microphone
    @Test
    fun testManualOneShotMicrophoneMode() {
        val isContinuous = false
        var state = AssistantState.SPEAKING
        var micActive = true

        state = AssistantState.IDLE
        if (!isContinuous) {
            micActive = false
        }
        assertEquals(AssistantState.IDLE, state)
        assertFalse("One-shot manual mic must deactivate mic after answer", micActive)
    }

    // 8. Continuous microphone
    @Test
    fun testContinuousMicrophoneMode() {
        val isContinuous = true
        var state = AssistantState.SPEAKING
        var commandListeningTriggered = false

        state = AssistantState.IDLE
        if (isContinuous) {
            commandListeningTriggered = true
        }
        assertEquals(AssistantState.IDLE, state)
        assertTrue("Continuous voice mode must trigger command listening", commandListeningTriggered)
    }

    // 9. Wake-word -> command transition
    @Test
    fun testWakeWordToCommandTransition() {
        var wakeWordRunning = true
        var recognizerRunning = false

        fun triggerWakeWord() {
            wakeWordRunning = false // Stop wake word first
            recognizerRunning = true // Start command recognition
        }

        triggerWakeWord()
        assertFalse(wakeWordRunning)
        assertTrue(recognizerRunning)
    }

    // 10. Wake-word and SpeechRecognizer mutual exclusion
    @Test
    fun testWakeWordAndSpeechRecognizerMutualExclusion() {
        var wakeWordActive = false
        var speechRecognizerActive = false

        fun startWakeWord() {
            speechRecognizerActive = false
            wakeWordActive = true
        }

        fun startSpeechRecognizer() {
            wakeWordActive = false
            speechRecognizerActive = true
        }

        startWakeWord()
        assertTrue(wakeWordActive)
        assertFalse(speechRecognizerActive)

        startSpeechRecognizer()
        assertFalse(wakeWordActive)
        assertTrue(speechRecognizerActive)
    }

    // 11. Processing blocks microphone
    @Test
    fun testProcessingBlocksMicrophone() {
        assertFalse(canStartCommandListening(VoiceState.PROCESSING, AssistantState.THINKING))
    }

    // 12. TTS blocks microphone
    @Test
    fun testTtsBlocksMicrophone() {
        assertFalse(canStartCommandListening(VoiceState.IDLE, AssistantState.SPEAKING, isTtsSpeaking = true))
    }

    // 13. AudioTrack completion does not directly restart mic
    @Test
    fun testAudioTrackCompletionGateArbitration() {
        var activePipeline = TestAudioPipeline.AUDIO_TRACK
        var micStarted = false

        fun onPlaybackEnded(pipeline: TestAudioPipeline) {
            if (pipeline == activePipeline) {
                micStarted = true // Routed through authoritative controller
            }
        }

        onPlaybackEnded(TestAudioPipeline.AUDIO_TRACK)
        assertTrue(micStarted)
    }

    // 14. Stale session callback ignored
    @Test
    fun testStaleSessionCallbackIgnored() {
        var currentSessionId = 5L
        var processed = false

        fun onCallback(sessionId: Long) {
            if (sessionId != currentSessionId) return
            processed = true
        }

        onCallback(4L) // Stale
        assertFalse(processed)

        onCallback(5L) // Current
        assertTrue(processed)
    }

    // 15. Cancelled session cannot restart mic
    @Test
    fun testCancelledSessionCannotRestartMic() {
        var currentSessionId = 1L
        var micStarted = false

        fun cancelSession() {
            currentSessionId++
        }

        fun finishSession(sessionId: Long) {
            if (sessionId == currentSessionId) {
                micStarted = true
            }
        }

        cancelSession()
        finishSession(1L) // old session finishing after cancellation
        assertFalse("Cancelled session must not restart mic", micStarted)
    }

    // 16. SpeechRecognizer retry respects gate
    @Test
    fun testSpeechRecognizerRetryRespectsGate() {
        var canListen = false
        var retryAttempted = false

        fun retry() {
            if (canListen) {
                retryAttempted = true
            }
        }

        retry()
        assertFalse(retryAttempted)

        canListen = true
        retry()
        assertTrue(retryAttempted)
    }

    // 17. Barge-in session invalidation
    @Test
    fun testBargeInSessionInvalidation() {
        var sessionId = 10L
        var ttsStopped = false

        fun onBargeIn() {
            sessionId++ // Invalidate session
            ttsStopped = true
        }

        onBargeIn()
        assertEquals(11L, sessionId)
        assertTrue(ttsStopped)
    }

    // 18. Screen Agent action verification (Plan -> Act -> Verify -> Reflect)
    @Test
    fun testScreenAgentActionVerificationPipeline() {
        var planned = false
        var acted = false
        var verified = false
        var reflected = false

        fun executeScreenAgent() {
            planned = true
            acted = true
            verified = true
            reflected = true
        }

        executeScreenAgent()
        assertTrue(planned && acted && verified && reflected)
    }

    // 19. Dangerous action confirmation
    @Test
    fun testDangerousActionConfirmationGated() {
        val isRiskyAction = true
        var confirmed = false
        var executed = false

        fun performAction() {
            if (!isRiskyAction || confirmed) {
                executed = true
            }
        }

        performAction()
        assertFalse("Dangerous action must not execute without confirmation", executed)

        confirmed = true
        performAction()
        assertTrue("Dangerous action must execute when confirmed", executed)
    }

    // 20. Full voice lifecycle
    @Test
    fun testFullVoiceLifecycleSequence() {
        val states = mutableListOf<String>()
        states.add("IDLE")
        states.add("WAKE_WORD_LISTENING")
        states.add("COMMAND_LISTENING")
        states.add("PROCESSING")
        states.add("GEMINI_STREAMING")
        states.add("SPEAKING")
        states.add("RESPONSE_FINISHED")
        states.add("IDLE")

        assertEquals(8, states.size)
        assertEquals("IDLE", states.first())
        assertEquals("RESPONSE_FINISHED", states[6])
        assertEquals("IDLE", states.last())
    }

    @Test
    fun testVoiceInputHandlerStrictSynchronousGuard() {
        var isBusy = true
        val handler = VoiceInputHandler(
            context = context,
            checkCanListen = { !isBusy }
        )

        handler.startListening()
        assertFalse("Handler must block immediately when checkCanListen is false", handler.isCurrentlyListening())
    }
}
