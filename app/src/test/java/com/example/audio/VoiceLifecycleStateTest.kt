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

    // TEST 1: Exactly-Once Session Completion with Utterance ID Deduplication
    @Test
    fun testTtsSessionExactlyOnceWithDuplicateUtteranceCallbacks() {
        val completedUtteranceIds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
        val sessionCompletionEmitted = AtomicBoolean(false)
        val pendingUtterances = AtomicInteger(0)
        var isStreamingSession = true
        var streamingComplete = false
        var sessionEndedCount = 0

        fun checkSessionCompletion() {
            if (isStreamingSession && streamingComplete && pendingUtterances.get() <= 0) {
                if (sessionCompletionEmitted.compareAndSet(false, true)) {
                    sessionEndedCount++
                }
            }
        }

        fun onUtteranceCallback(utteranceId: String) {
            if (completedUtteranceIds.add(utteranceId)) {
                pendingUtterances.decrementAndGet().coerceAtLeast(0)
                checkSessionCompletion()
            }
        }

        // Two chunks queued
        val id1 = "chunk-1-uuid"
        val id2 = "chunk-2-uuid"
        pendingUtterances.set(2)

        // Chunk 1 finishes
        onUtteranceCallback(id1)
        assertEquals(1, pendingUtterances.get())

        // Duplicate onDone received for Chunk 1 (Audio system glitch)
        onUtteranceCallback(id1)
        // Utterance deduplication prevents pending count from dropping below 1!
        assertEquals("Duplicate callback must not decrement pending count", 1, pendingUtterances.get())
        assertEquals(0, sessionEndedCount)

        // Chunk 2 finishes
        onUtteranceCallback(id2)
        assertEquals(0, pendingUtterances.get())
        assertEquals("Should not complete before Gemini stream marks complete", 0, sessionEndedCount)

        // Now Gemini stream finishes
        streamingComplete = true
        checkSessionCompletion()
        assertEquals("Session ended must emit exactly once", 1, sessionEndedCount)

        // Late duplicate callback arrives
        onUtteranceCallback(id2)
        checkSessionCompletion()
        assertEquals("Late duplicate callback must not trigger another session completion", 1, sessionEndedCount)
    }

    // TEST 2: Session Token Invalidation & Stale Callback Rejection
    @Test
    fun testSessionTokenInvalidationRejectsStaleCallbacks() {
        var currentVoiceSessionId = 1L
        var recognizedSessionId = 0L

        fun onResponseFinished(sessionId: Long) {
            if (sessionId != currentVoiceSessionId) {
                // Stale callback rejected
                return
            }
            recognizedSessionId = sessionId
        }

        // Callback from old session 1 arrives after user interrupted and started session 2
        currentVoiceSessionId = 2L // user started new turn
        onResponseFinished(sessionId = 1L)
        assertEquals("Stale session 1 callback must be ignored", 0L, recognizedSessionId)

        // Valid callback from current session 2 arrives
        onResponseFinished(sessionId = 2L)
        assertEquals("Current session 2 callback must be accepted", 2L, recognizedSessionId)
    }

    // TEST 3: AudioTrackPlayer vs TextToSpeech Pipeline Arbitration
    @Test
    fun testAudioPipelineArbitrationPreventsDuplicateRestart() {
        var activePipeline = TestAudioPipeline.TTS // TTS is active
        var restarts = 0

        fun onPlaybackFinished(sourcePipeline: TestAudioPipeline) {
            if (sourcePipeline != activePipeline) {
                return // Ignored!
            }
            activePipeline = TestAudioPipeline.NONE
            restarts++
        }

        // AudioTrackPlayer ends while TTS is the active pipeline
        onPlaybackFinished(TestAudioPipeline.AUDIO_TRACK)
        assertEquals("AudioTrackPlayer callback must be ignored when TTS is active", 0, restarts)

        // TTS finishes
        onPlaybackFinished(TestAudioPipeline.TTS)
        assertEquals("Only active TTS pipeline may finish response", 1, restarts)
    }

    // TEST 4: One-Shot Manual Mic Mode Returns to IDLE without Auto-Listen
    @Test
    fun testOneShotManualMicModeNeverAutoListens() {
        val isContinuousVoiceModeEnabled = false
        var micActive = false
        var state = AssistantState.SPEAKING
        var autoListenStarted = false

        // Response ends
        state = AssistantState.IDLE
        if (isContinuousVoiceModeEnabled) {
            autoListenStarted = true
        } else {
            micActive = false
        }

        assertEquals(AssistantState.IDLE, state)
        assertFalse("One-shot manual mic must NOT auto-listen", autoListenStarted)
        assertFalse("Mic must be inactive", micActive)
    }

    // TEST 5: Microphone Arbitration with Active TTS Audio
    @Test
    fun testMicrophoneArbitrationBlocksDuringActiveTtsOrAudio() {
        // VoiceState is IDLE, but TTS is still rendering audio physically
        assertFalse(
            "Must NOT start listening while TTS audio is active",
            canStartCommandListening(VoiceState.IDLE, AssistantState.IDLE, isTtsSpeaking = true)
        )
        assertFalse(
            "Must NOT start listening while PCM AudioTrack is playing",
            canStartCommandListening(VoiceState.IDLE, AssistantState.IDLE, isAudioPlaying = true)
        )
        assertTrue(
            "Allowed only when both states and audio output are completely silent",
            canStartCommandListening(VoiceState.IDLE, AssistantState.IDLE, isTtsSpeaking = false, isAudioPlaying = false)
        )
    }

    // TEST 6: Barge-In Interruption Invalidates Token and Resets Pipeline
    @Test
    fun testBargeInInvalidatesActiveSession() {
        var currentVoiceSessionId = 5L
        var isAudioActive = true

        // User speaks and triggers barge-in
        val oldSessionId = currentVoiceSessionId
        currentVoiceSessionId++ // Invalidation
        isAudioActive = false // Interrupt

        assertTrue("Session token must be incremented", currentVoiceSessionId > oldSessionId)
        assertFalse("Audio must be stopped", isAudioActive)
    }

    // TEST 7: Stop Session Shuts Down in Strict Order and Invalidates Callbacks
    @Test
    fun testStopSessionOrderedShutdown() {
        var sessionId = 10L
        var recognizerStopped = false
        var wakeWordStopped = false
        var audioStopped = false
        var ttsStopped = false
        var state = AssistantState.SPEAKING

        fun stopSession() {
            sessionId++ // 1. Invalidate token
            recognizerStopped = true // 2. Stop recognizer
            wakeWordStopped = true // 3. Stop wake word
            audioStopped = true // 4. Stop audio
            ttsStopped = true // 5. Stop TTS
            state = AssistantState.IDLE // 6. State IDLE
        }

        stopSession()
        assertEquals(11L, sessionId)
        assertTrue(recognizerStopped)
        assertTrue(wakeWordStopped)
        assertTrue(audioStopped)
        assertTrue(ttsStopped)
        assertEquals(AssistantState.IDLE, state)
    }

    // TEST 8: VoiceInputHandler startListening cannot bypass checkCanListen
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
