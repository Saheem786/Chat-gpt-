package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Hardened Text-to-Speech (TTS) Engine wrapping Android's TextToSpeech class.
 * Features:
 * - Deterministic TTS Session Lifecycle
 * - Exactly-Once session completion emission (sessionCompletionEmitted gate)
 * - Utterance ID deduplication via thread-safe completedUtteranceIds set
 * - Safe initialization fallback through centralized completion gate
 * - Dynamic language & locale fallback (Hindi / Indian English / US English)
 */
class TextToSpeechEngine(
    context: Context,
    private val scope: CoroutineScope,
    private val onSpeechStarted: () -> Unit = {},
    private val onSpeechEnded: () -> Unit = {},
    private val onSpeechSessionEnded: () -> Unit = onSpeechEnded,
    private val onAmplitudeChanged: (Float) -> Unit = {},
    private val onDiagnosticLog: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "SamTextToSpeechEngine"
    }

    private var tts: TextToSpeech? = null
    private val isInitialized = AtomicBoolean(false)
    private val isSpeaking = AtomicBoolean(false)
    
    // TTS Session state tracking
    private var isStreamingSession = false
    private var streamingComplete = false
    private val pendingUtterances = AtomicInteger(0)
    private val sessionCompletionEmitted = AtomicBoolean(false)
    private val completedUtteranceIds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private var amplitudeJob: Job? = null
    private var pendingSpeechText: String? = null
    private var currentLanguage: AssistantLanguage = AssistantLanguage.HINGLISH
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentVoiceType: String = "FEMALE"

    fun setLanguage(language: AssistantLanguage) {
        currentLanguage = language
        configureLocaleAndVoice()
    }

    fun setVoiceType(voiceType: String) {
        currentVoiceType = voiceType
        configureLocaleAndVoice()
    }

    init {
        mainHandler.post {
            try {
                onDiagnosticLog("Initializing Android TextToSpeech engine on Main thread...")
                tts = TextToSpeech(context.applicationContext, this@TextToSpeechEngine)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to instantiate Android TextToSpeech", e)
                onDiagnosticLog("TTS creation error: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized.set(true)
            onDiagnosticLog("Android TextToSpeech engine initialized successfully")
            Log.i(TAG, "TextToSpeech initialized successfully")
            configureLocaleAndVoice()
            setupUtteranceListener()

            pendingSpeechText?.let { queuedText ->
                pendingSpeechText = null
                speak(queuedText)
            }
        } else {
            isInitialized.set(false)
            val errorMsg = "TTS initialization failed with status code: $status"
            Log.e(TAG, errorMsg)
            onDiagnosticLog(errorMsg)
            // Centralized completion fallback
            completeTtsSession()
        }
    }

    private fun configureLocaleAndVoice() {
        val currentTts = tts ?: return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            currentTts.setAudioAttributes(audioAttributes)
        } catch (e: Exception) {
            Log.w(TAG, "AudioAttributes configuration fallback", e)
        }

        val targetLocale = currentLanguage.ttsLocale
        val result = currentTts.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "${currentLanguage.displayName} Locale not fully supported, falling back to Indian English/Hindi")
            val inResult = currentTts.setLanguage(Locale("en", "IN"))
            if (inResult == TextToSpeech.LANG_MISSING_DATA || inResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                currentTts.setLanguage(Locale("hi", "IN"))
            }
        }

        try {
            val availableVoices = currentTts.voices
            if (!availableVoices.isNullOrEmpty()) {
                val selectedVoice = availableVoices.firstOrNull { voice ->
                    val nameLower = voice.name.lowercase()
                    nameLower.contains("hi-in") && nameLower.contains(currentVoiceType.lowercase()) &&
                    !voice.isNetworkConnectionRequired
                } ?: availableVoices.firstOrNull { voice ->
                    val nameLower = voice.name.lowercase()
                    nameLower.contains("hi-in-x") || nameLower.contains("en-in-x") ||
                    nameLower.contains("hi_in") || nameLower.contains("en_in")
                } ?: availableVoices.firstOrNull { voice ->
                    voice.locale == Locale("hi", "IN") || voice.locale == Locale("en", "IN")
                } ?: availableVoices.firstOrNull { voice ->
                    voice.locale.language == currentLanguage.ttsLanguage
                }

                selectedVoice?.let {
                    currentTts.voice = it
                    onDiagnosticLog("Assigned voice style ($currentVoiceType): ${it.name} (${it.locale})")
                    Log.i(TAG, "Selected natural human voice style: ${it.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply natural voice selection", e)
        }

        val pitch = when (currentVoiceType) {
            "FEMALE" -> 1.18f
            "MALE" -> 0.86f
            else -> 1.02f
        }
        currentTts.setPitch(pitch)
        currentTts.setSpeechRate(0.96f)
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking.set(true)
                scope.launch(Dispatchers.Main) {
                    onSpeechStarted()
                    startAmplitudeSimulation()
                }
                onDiagnosticLog("TTS speech started (id: $utteranceId)")
            }

            override fun onDone(utteranceId: String?) {
                onDiagnosticLog("TTS utterance finished (id: $utteranceId)")
                handleUtteranceCompletion(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onDiagnosticLog("TTS utterance error on utterance: $utteranceId")
                handleUtteranceCompletion(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onDiagnosticLog("TTS utterance error code $errorCode on utterance: $utteranceId")
                handleUtteranceCompletion(utteranceId)
            }
        })
    }

    private fun handleUtteranceCompletion(utteranceId: String?) {
        val id = utteranceId ?: UUID.randomUUID().toString()
        // Section 3: Utterance ID Deduplication - mutually exclusive with onDone/onError
        if (completedUtteranceIds.add(id)) {
            val remaining = pendingUtterances.decrementAndGet().coerceAtLeast(0)
            checkSessionCompletion(remaining)
        } else {
            onDiagnosticLog("TTS duplicate callback ignored for utterance: $id")
        }
    }

    fun speakWithIndianAccent(
        rawText: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        utteranceId: String = UUID.randomUUID().toString()
    ) {
        speak(rawText, queueMode, utteranceId)
    }

    fun speakChunk(
        chunkText: String,
        isFirstChunk: Boolean
    ) {
        val clean = sanitizeTextForSpeech(chunkText)
        if (clean.isBlank()) return

        synchronized(this) {
            if (isFirstChunk) {
                isStreamingSession = true
                streamingComplete = false
                sessionCompletionEmitted.set(false)
                completedUtteranceIds.clear()
                pendingUtterances.set(0)
            }
            pendingUtterances.incrementAndGet()
        }

        val queueMode = if (isFirstChunk) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        speakInternal(clean, queueMode, UUID.randomUUID().toString())
    }

    fun markStreamingResponseComplete() {
        onDiagnosticLog("TTS markStreamingResponseComplete called. Pending utterances: ${pendingUtterances.get()}")
        var shouldComplete = false
        synchronized(this) {
            streamingComplete = true
            if (pendingUtterances.get() <= 0) {
                shouldComplete = true
            }
        }
        if (shouldComplete) {
            completeTtsSession()
        }
    }

    fun speak(
        rawText: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        utteranceId: String = UUID.randomUUID().toString()
    ) {
        val cleanText = sanitizeTextForSpeech(rawText)
        if (cleanText.isBlank()) return

        synchronized(this) {
            isStreamingSession = false
            streamingComplete = true
            sessionCompletionEmitted.set(false)
            completedUtteranceIds.clear()
            pendingUtterances.set(1)
        }
        speakInternal(cleanText, queueMode, utteranceId)
    }

    private fun speakInternal(cleanText: String, queueMode: Int, utteranceId: String) {
        if (!isInitialized.get()) {
            onDiagnosticLog("TTS not ready yet, queueing message: '${cleanText.take(30)}...'")
            pendingSpeechText = cleanText
            scope.launch(Dispatchers.Main) {
                delay(2500L)
                if (!isInitialized.get() && pendingSpeechText != null) {
                    onDiagnosticLog("TTS init timeout reached. Triggering speech completion fallback.")
                    pendingSpeechText = null
                    completeTtsSession()
                }
            }
            return
        }

        try {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            val result = tts?.speak(cleanText, queueMode, params, utteranceId)
            if (result == TextToSpeech.SUCCESS) {
                onDiagnosticLog("TTS queued utterance: '${cleanText.take(40)}...'")
            } else {
                onDiagnosticLog("TTS speak call returned status: $result (fallback end)")
                handleUtteranceCompletion(utteranceId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking TextToSpeech.speak", e)
            onDiagnosticLog("TTS speak error: ${e.message}")
            handleUtteranceCompletion(utteranceId)
        }
    }

    private fun checkSessionCompletion(remaining: Int) {
        var shouldComplete = false
        synchronized(this) {
            if (isStreamingSession) {
                if (streamingComplete && remaining <= 0) {
                    shouldComplete = true
                }
            } else {
                if (remaining <= 0) {
                    shouldComplete = true
                }
            }
        }
        if (shouldComplete) {
            completeTtsSession()
        }
    }

    private fun completeTtsSession() {
        // Section 2: Exactly-Once session completion gate
        if (sessionCompletionEmitted.compareAndSet(false, true)) {
            synchronized(this) {
                isSpeaking.set(false)
                isStreamingSession = false
                streamingComplete = false
                pendingUtterances.set(0)
            }
            stopAmplitudeSimulation()
            onDiagnosticLog("TTS SESSION COMPLETE [Exactly-Once event emitted]")
            scope.launch(Dispatchers.Main) {
                onSpeechEnded()
                onSpeechSessionEnded()
            }
        } else {
            onDiagnosticLog("TTS completeTtsSession called but session completion already emitted for this session")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        synchronized(this) {
            isSpeaking.set(false)
            isStreamingSession = false
            streamingComplete = false
            pendingUtterances.set(0)
        }
        stopAmplitudeSimulation()
        if (sessionCompletionEmitted.compareAndSet(false, true)) {
            scope.launch(Dispatchers.Main) {
                onSpeechEnded()
                onSpeechSessionEnded()
            }
        }
        onDiagnosticLog("TTS stopped")
    }

    fun isSpeakingNow(): Boolean {
        return isSpeaking.get() || (tts?.isSpeaking == true)
    }

    private fun startAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch(Dispatchers.Default) {
            var phase = 0.0
            while (isActive && isSpeaking.get()) {
                phase += 0.35
                val wave = (Math.sin(phase) + 1.0) / 2.0
                val jitter = Random.nextDouble(0.15, 0.45)
                val amp = ((wave * 0.5 + jitter * 0.5) * 0.85).coerceIn(0.15, 0.95).toFloat()
                scope.launch(Dispatchers.Main) {
                    if (isSpeaking.get()) {
                        onAmplitudeChanged(amp)
                    }
                }
                delay(60)
            }
            scope.launch(Dispatchers.Main) {
                onAmplitudeChanged(0f)
            }
        }
    }

    private fun stopAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        onAmplitudeChanged(0f)
    }

    private fun sanitizeTextForSpeech(text: String): String {
        return text
            .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
            .replace(Regex("""\*(.*?)\*"""), "$1")
            .replace(Regex("""`{1,3}(.*?)`{1,3}"""), "$1")
            .replace(Regex("""(?m)^#+\s*"""), "")
            .replace(Regex("""https?://\S+"""), "link")
            .replace(Regex("""[\uD83C-\uDBFF\uDC00-\uDFFF]+"""), "")
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .trim()
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TextToSpeech", e)
        }
        tts = null
        isInitialized.set(false)
        onDiagnosticLog("Android TextToSpeech engine released")
    }
}
