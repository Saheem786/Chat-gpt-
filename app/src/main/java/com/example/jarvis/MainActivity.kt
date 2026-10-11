package com.example.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var statusText: TextView
    private lateinit var btnListen: Button
    private lateinit var avatarVideoView: VideoView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private val wakeWord = "jarvis"
    private val executor = Executors.newSingleThreadExecutor()

    // API KEY DAALEIN: Yahan apni Gemini API key paste karein
    private val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"

    // Yahan maine aapka diya hua link add kar diya hai
    private val avatarImgUrl = "https://i.postimg.cc/WhJXDqht/image.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnListen = findViewById(R.id.btnListen)
        avatarVideoView = findViewById(R.id.avatarVideoView)
        
        tts = TextToSpeech(this, this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(recognitionListener)

        btnListen.setOnClickListener { startListening() }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { statusText.text = "Listening..." }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { statusText.text = "Processing..." }
        override fun onError(error: Int) { statusText.text = "STT error: $error" }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isBlank()) return

            val spokenLower = spoken.lowercase(Locale.getDefault())
            statusText.text = "Heard: $spoken"

            if (!spokenLower.contains(wakeWord)) {
                speak("Say Jarvis followed by your command.")
                return
            }

            val idx = spokenLower.indexOf(wakeWord)
            val command = spoken.substring(idx + wakeWord.length).trim()

            if (command.isBlank()) {
                speak("Yes, Sir? How can I help?")
                return
            }

            executor.execute {
                try {
                    val client = GeminiClient()
                    val reply = client.generateResponse(GEMINI_API_KEY, command)
                    
                    runOnUiThread { statusText.text = "Animating Avatar..." }
                    
                    val didClient = DIDClient()
                    val videoUrl = didClient.generateLipSyncVideo(avatarImgUrl, reply)

                    runOnUiThread {
                        statusText.text = "Jarvis: $reply"
                        if (videoUrl != null) {
                            avatarVideoView.setVideoURI(Uri.parse(videoUrl))
                            avatarVideoView.start()
                        } else {
                            speak(reply)
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        statusText.text = "Error connecting to AI."
                        speak("I encountered an error, Sir.")
                    }
                }
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer.startListening(intent)
    }

    private fun speak(text: String) {
        if (text.isNotBlank()) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            tts.setSpeechRate(1.0f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.shutdown()
        executor.shutdown()
    }
}
