package com.example.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Robust Speech Recognition (STT) Manager using Android's SpeechRecognizer API.
 * Supports:
 * - Free-form Indian English / Hindi (Hinglish) speech recognition
 * - Auto-silence detection and automatic sentence endpointing
 * - Real-time partial results and RMS amplitude callbacks for UI wave visualization
 * - Auto-reconnection / error resilience for continuous hands-free voice mode
 * - Safe Main-thread execution with permission guards
 */
class VoiceInputHandler(
    private val context: Context,
    private val checkCanListen: () -> Boolean = { true },
    private val onSpeechReady: () -> Unit = {},
    private val onSpeechStarted: () -> Unit = {},
    private val onRmsAmplitudeChanged: (amplitude: Float) -> Unit = {},
    private val onPartialTranscript: (partialText: String) -> Unit = {},
    private val onFinalTranscript: (finalText: String) -> Unit = {},
    private val onErrorOccurred: (errorMessage: String, isTimeout: Boolean) -> Unit = { _, _ -> },
    private val onSpeechEnded: () -> Unit = {},
    private val onDiagnosticLog: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "SamVoiceInputHandler"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isDestroyed = false
    private var retryCount = 0
    private var currentLanguage: AssistantLanguage = AssistantLanguage.HINGLISH

    fun setLanguage(language: AssistantLanguage) {
        runOnMain {
            val previous = currentLanguage
            currentLanguage = language
            onDiagnosticLog("VoiceInputHandler: Dynamic language configuration updated to ${language.displayName} (${language.speechLocale}) [${language.accuracyBadge}]")
            if (isListening && previous != language) {
                // Safely cancel and restart with updated recognition intent
                try {
                    speechRecognizer?.cancel()
                    speechRecognizer?.destroy()
                    speechRecognizer = null
                } catch (e: Exception) {
                    Log.w(TAG, "SpeechRecognizer cancel on language change", e)
                }
                isListening = false
                mainHandler.postDelayed({
                    if (!isDestroyed) {
                        startListening()
                    }
                }, 200L)
            } else if (!isListening && previous != language) {
                try {
                    speechRecognizer?.destroy()
                    speechRecognizer = null
                } catch (e: Exception) {
                    Log.w(TAG, "SpeechRecognizer destroy on language change", e)
                }
            }
        }
    }

    fun getLanguage(): AssistantLanguage = currentLanguage

    internal fun buildRecognizerIntent(targetLanguage: AssistantLanguage = currentLanguage): Intent {
        val primaryLang = targetLanguage.speechLocale
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            when (targetLanguage) {
                AssistantLanguage.HINGLISH -> {
                    // Bi-directional Hindi + English code-switching
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    val additional = arrayOf("en-IN", "hi-IN", "en-US", "hi")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", additional)
                    putExtra("android.speech.extra.LANGUAGE_DETECTION_ALLOWED_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                    putExtra("android.speech.extra.LANGUAGE_SWITCH_ALLOWED_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                    putExtra("android.speech.extra.ENABLE_LANGUAGE_DETECTION", true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 400L)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening in Hinglish (Hindi + English)...")
                }
                AssistantLanguage.HINDI -> {
                    // Pure Devanagari Hindi acoustic & language model
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "hi"))
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 400L)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "हिन्दी में बोलें...")
                }
                AssistantLanguage.ENGLISH_IN -> {
                    // Indian English Phonetics & Accent Tuning
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 400L)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak English (Indian Accent)...")
                }
                AssistantLanguage.ENGLISH_US -> {
                    // Standard General American English model
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS...")
                }
            }
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            onSpeechReady()
        }

        override fun onBeginningOfSpeech() {
            onSpeechStarted()
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Logarithmic mapping of voice amplitudes for Compose UI ripple wave rendering
            val normalized = (rmsdB + 2.0f).coerceIn(0.0f, 12.0f) / 12.0f
            onRmsAmplitudeChanged(normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Raw PCM audio buffer chunk received
        }

        override fun onEndOfSpeech() {
            isListening = false
            onSpeechEnded()
        }

        override fun onError(errorCode: Int) {
            isListening = false
            onRmsAmplitudeChanged(0f)

            // Ignore code 5 (ERROR_CLIENT) which frequently occurs during cancel/destroy lifecycle sweeps
            if (errorCode == 5) {
                onDiagnosticLog("SpeechRecognizer ERROR_CLIENT (code 5) suppressed during state transition")
                Log.w(TAG, "Suppressed ERROR_CLIENT (code 5) from SpeechRecognizer")
                return
            }

            // Auto-recovery for Error 11 (ERROR_SERVER_DISCONNECTED) and Error 8 (ERROR_RECOGNIZER_BUSY)
            if ((errorCode == SpeechRecognizer.ERROR_SERVER_DISCONNECTED || errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) && retryCount < 3 && !isDestroyed) {
                retryCount++
                onDiagnosticLog("Speech service disconnected/busy (code $errorCode). Auto-reconnecting attempt $retryCount/3...")
                mainHandler.postDelayed({
                    if (!isDestroyed) {
                        startListening()
                    }
                }, 350L * retryCount)
                return
            }

            // Gently handle ERROR_NO_MATCH without alarming error messages
            if (errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
                onDiagnosticLog("SpeechRecognizer: No match detected. Soft reset.")
                onErrorOccurred("Listening...", true)
                return
            }

            val isTimeout = errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            val errorMsg = when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions for speech recognition"
                SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during speech recognition"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timed out"
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Recognition service disconnected. Tap mic to retry."
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported by device"
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language pack unavailable"
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests to speech service"
                else -> "Speech recognition error code: $errorCode"
            }
            onDiagnosticLog("SpeechRecognizer error ($errorCode): $errorMsg")
            Log.w(TAG, "SpeechRecognizer error: $errorMsg (code: $errorCode)")
            onErrorOccurred(errorMsg, isTimeout)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            retryCount = 0
            onRmsAmplitudeChanged(0f)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim().orEmpty()
            
            onDiagnosticLog("VOICE_STATE: FINAL_RESULT [RECOGNIZER_FINAL]")
            onDiagnosticLog("SpeechRecognizer final result: '$recognizedText'")
            
            if (recognizedText.isNotBlank()) {
                onFinalTranscript(recognizedText)
            } else {
                onErrorOccurred("No speech recognized", true)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull()?.trim().orEmpty()
            if (partialText.isNotBlank()) {
                onPartialTranscript(partialText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Extra events
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening() {
        if (isDestroyed) return
        runOnMain {
            if (!checkCanListen()) {
                onDiagnosticLog("VOICE_STATE: BLOCKED [RECOGNIZER_START BLOCKED by assistant busy state]")
                return@runOnMain
            }
            
            if (!hasRecordAudioPermission()) {
                val err = "Microphone permission is required for voice recognition."
                onDiagnosticLog("Start listening aborted: $err")
                onErrorOccurred(err, false)
                return@runOnMain
            }
            if (!isRecognitionAvailable()) {
                val err = "Speech recognition service is not available on this device."
                onDiagnosticLog("Speech recognition unavailable")
                onErrorOccurred(err, false)
                return@runOnMain
            }
            try {
                if (isListening) {
                    isListening = false
                    try {
                        speechRecognizer?.cancel()
                    } catch (e: Exception) {
                        Log.w(TAG, "SpeechRecognizer cancel before restart", e)
                    }
                }
                speechRecognizer?.destroy()
                speechRecognizer = null
                // For Hindi and Hinglish, standard SpeechRecognizer uses Google Cloud neural ASR which is vastly superior
                speechRecognizer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context) &&
                    currentLanguage != AssistantLanguage.HINGLISH &&
                    currentLanguage != AssistantLanguage.HINDI) {
                    try {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context).apply {
                            setRecognitionListener(recognitionListener)
                        }
                    } catch (e: Exception) {
                        SpeechRecognizer.createSpeechRecognizer(context).apply {
                            setRecognitionListener(recognitionListener)
                        }
                    }
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(recognitionListener)
                    }
                }
                val intent = buildRecognizerIntent()
                speechRecognizer?.startListening(intent)
                
                onDiagnosticLog("RECOGNIZER_START")
                onDiagnosticLog("SpeechRecognizer started listening (${currentLanguage.displayName} - ${currentLanguage.speechLocale} - ${currentLanguage.accuracyBadge})")
            } catch (e: SecurityException) {
                Log.e(TAG, "Microphone permission denied for SpeechRecognizer", e)
                onDiagnosticLog("SecurityException: Microphone permission not granted")
                onErrorOccurred("Microphone permission required", false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognizer", e)
                onDiagnosticLog("Failed to start SpeechRecognizer: ${e.message}")
                onErrorOccurred("Voice error: ${e.localizedMessage ?: e.message}", false)
            }
        }
    }

    fun stopListening() {
        runOnMain {
            if (isListening) {
                isListening = false
                try {
                    speechRecognizer?.stopListening()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping SpeechRecognizer", e)
                }
                onDiagnosticLog("RECOGNIZER_STOP")
            }
            onRmsAmplitudeChanged(0f)
        }
    }

    fun cancel() {
        runOnMain {
            try {
                isListening = false
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling SpeechRecognizer", e)
            }
            onRmsAmplitudeChanged(0f)
        }
    }

    fun destroy() {
        isDestroyed = true
        runOnMain {
            try {
                isListening = false
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying SpeechRecognizer", e)
            }
        }
    }

    fun isCurrentlyListening(): Boolean = isListening
}
