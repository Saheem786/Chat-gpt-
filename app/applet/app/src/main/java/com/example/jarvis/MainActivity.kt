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
import android.speech.tts.UtteranceProgressListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.Executors
import com.example.jarvis.BuildConfig

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var statusText: TextView
    private lateinit var btnListen: Button
    private lateinit var avatarVideoView: VideoView
    private lateinit var visualizer: VoiceVisualizer
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private val wakeWord = "jarvis"
    private val executor = Executors.newSingleThreadExecutor()

    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    private val avatarImgUrl = "https://i.postimg.cc/WhJXDqht/image.jpg"
    private lateinit var glowAnimation: Animation

    private fun setVisualFeedback(active: Boolean) {
        runOnUiThread {
            if (active) {
                btnListen.startAnimation(glowAnimation)
                avatarVideoView.alpha = 0.8f
            } else {
                btnListen.clearAnimation()
                avatarVideoView.alpha = 1.0f
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnListen = findViewById(R.id.btnListen)
        avatarVideoView = findViewById(R.id.avatarVideoView)
        visualizer = findViewById(R.id.visualizer)
        
        glowAnimation = AnimationUtils.loadAnimation(this, R.anim.glow_animation)
        
        tts = TextToSpeech(this, this)

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { statusText.text = "Listening..." }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            if (PermissionManager.hasRecordAudioPermission(this@MainActivity)) {
                visualizer.setAmplitude(rmsdB / 10f)
            }
        }
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

            // Apply entry animation
            runOnUiThread {
                val entryAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.avatar_entry)
                avatarVideoView.startAnimation(entryAnim)
            }

            val idx = spokenLower.indexOf(wakeWord)
            val command = spoken.substring(idx + wakeWord.length).trim()

            if (command.isBlank()) {
                speak("Yes, Sir? How can I help?")
                return
            }

            // Parse for system commands first
            if (CommandLauncher.launchApp(this@MainActivity, command)) {
                statusText.text = "Launching..."
                speak("Opening that for you, Sir.")
                return
            }

            executor.execute {
                setVisualFeedback(true)
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
                } finally {
                    setVisualFeedback(false)
                }
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun startListening() {
        if (PermissionManager.hasRecordAudioPermission(this)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer.startListening(intent)
        } else {
            statusText.text = "Permission required."
        }
    }

    private fun speak(text: String) {
        if (text.isNotBlank()) {
            val prefs = getSharedPreferences("JarvisPrefs", Context.MODE_PRIVATE)
            val rate = prefs.getFloat("rate", 1.0f)
            val pitch = prefs.getFloat("pitch", 1.0f)
            tts.setSpeechRate(rate)
            tts.setPitch(pitch)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            tts.setSpeechRate(1.0f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    runOnUiThread { visualizer.setAmplitude(0.5f) }
                }
                override fun onDone(utteranceId: String?) {
                    runOnUiThread { visualizer.setAmplitude(0f) }
                }
                override fun onError(utteranceId: String?) {
                    runOnUiThread { visualizer.setAmplitude(0f) }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.shutdown()
        executor.shutdown()
    }
}
