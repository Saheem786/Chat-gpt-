package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread
import kotlin.math.sqrt

class SamWakeWordService : Service() {

    companion object {
        private const val TAG = "SamWakeWordService"
        private const val CHANNEL_ID = "SamWakeWordChannel"
        private const val NOTIFICATION_ID = 42

        const val ACTION_WAKE_WORD_TRIGGERED = "com.example.sam.ACTION_WAKE_WORD_TRIGGERED"
        const val EXTRA_DETECTED_KEYWORD = "extra_detected_keyword"

        // Audio parameters matching VoiceMatchManager
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var isListening = false
    private var audioRecord: AudioRecord? = null
    private var voiceMatchManager: VoiceMatchManager? = null
    private var keywordSpotter = LightweightKeywordSpotter()
    private var vibrator: Vibrator? = null

    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var isScreenOff = false

    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOff = true
                    Log.i(TAG, "Screen off. Low-Power Standby Mode triggered.")
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOff = false
                    Log.i(TAG, "Screen on. Normal Power Mode restored.")
                }
            }
        }
    }

    private fun isPowerSaveMode(): Boolean {
        return try {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isPowerSaveMode
        } catch (e: Exception) {
            false
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "SamAI::WakeWordWakeLock").apply {
                acquire(10 * 60 * 1000L) // Limit to 10 minutes maximum to optimize battery standby
            }
            Log.d(TAG, "Standby WakeLock acquired.")
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock acquisition failed", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock release failed", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        voiceMatchManager = VoiceMatchManager(this)
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val screenFilter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, screenFilter)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }

        val hasPermission = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.e(TAG, "Cannot start SamWakeWordService. RECORD_AUDIO permission missing.")
            // Safety: Always call startForeground first to prevent ForegroundServiceDidNotStartInTimeException, then stopSelf.
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Try starting with MICROPHONE type first so it complies with manifest
                    startForeground(
                        NOTIFICATION_ID, 
                        createNotification(), 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on fallback startForeground when permission is missing", e)
                try {
                    // Try fallback startForeground without type
                    startForeground(NOTIFICATION_ID, createNotification())
                } catch (ex: SecurityException) {
                    Log.e(TAG, "SecurityException on absolute fallback startForeground", ex)
                }
            }
            stopSelf()
            return START_NOT_STICKY
        }

        acquireWakeLock()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    createNotification(), 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting foreground service", e)
            stopSelf()
            return START_NOT_STICKY
        }
        
        startWakeWordListener()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startWakeWordListener() {
        if (isListening) return
        isListening = true

        val hasPermission = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.e(TAG, "Cannot start wake word listener. RECORD_AUDIO permission missing.")
            stopSelf()
            return
        }

        thread(start = true, name = "SamWakeWordThread") {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(3200)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord could not be initialized.")
                    isListening = false
                    return@thread
                }

                audioRecord?.startRecording()
                val prefs = getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
                val targetKey = prefs.getString("wake_word_option", WakeWordOption.HEY_ARUSHI.key)
                val option = WakeWordOption.fromKey(targetKey)
                Log.i(TAG, "Continuous wake word service is listening for '${option.phrase}'...")

                val buffer = ShortArray(1600) // 100ms frames
                val audioWindow = ByteArrayOutputStream()

                while (isListening) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        // Calculate RMS Energy
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read)
                        val normalizedRms = rms / 32768.0

                        // Voice Activity Detection (VAD) threshold (e.g. 0.015f / ~45dB)
                        if (normalizedRms > 0.015) {
                            // Convert short buffer to byte buffer
                            val byteBuffer = ByteArray(read * 2)
                            for (i in 0 until read) {
                                val value = buffer[i]
                                byteBuffer[i * 2] = (value.toInt() and 0xFF).toByte()
                                byteBuffer[i * 2 + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
                            }
                            audioWindow.write(byteBuffer)

                            // Keep window length max ~2 seconds (64000 bytes @ 16kHz 16-bit Mono)
                            if (audioWindow.size() > 64000) {
                                val currentBytes = audioWindow.toByteArray()
                                audioWindow.reset()
                                audioWindow.write(currentBytes, currentBytes.size - 32000, 32000)
                            }
                        } else {
                            // Silence / Pause detected - check if we accumulated enough speech window to evaluate
                            if (audioWindow.size() >= 16000) { // at least 500ms of audio
                                val pcmData = audioWindow.toByteArray()
                                audioWindow.reset()

                                Log.d(TAG, "Evaluating speech segment for wake word activation...")
                                evaluateSpeechSegment(pcmData)
                            }
                        }
                    }
                    val sleepMs = when {
                        isPowerSaveMode() -> 1500L
                        isScreenOff -> 150L
                        else -> 50L
                    }
                    Thread.sleep(sleepMs)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error inside wake word detection loop", e)
            } finally {
                stopRecordingQuietly()
            }
        }
    }

    private fun evaluateSpeechSegment(pcmData: ByteArray) {
        val prefs = getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        val targetKey = prefs.getString("wake_word_option", WakeWordOption.HEY_ARUSHI.key)
        val option = WakeWordOption.fromKey(targetKey)
        val sensitivity = prefs.getFloat("wake_word_sensitivity", 0.60f)

        // Evaluate acoustic keyword match using LightweightKeywordSpotter
        val spotterResult = keywordSpotter.evaluatePcm(pcmData, option, sensitivity)
        Log.d(TAG, "Keyword Spotter result: ${spotterResult.details}, isMatch=${spotterResult.isMatch}, conf=${spotterResult.confidence}")

        if (!spotterResult.isMatch) {
            return
        }

        val enforceVoiceMatch = prefs.getBoolean("voice_match_enforced", false)
        if (enforceVoiceMatch) {
            val similarity = voiceMatchManager?.verifySpeaker(pcmData) ?: 0.0f
            Log.d(TAG, "Voice Match verification score: $similarity")
            if (similarity >= 0.65f) {
                triggerWakeUp(spotterResult.detectedKeyword)
            } else {
                Log.d(TAG, "Speaker mismatch for voice match. Wake word rejected.")
            }
        } else {
            triggerWakeUp(spotterResult.detectedKeyword)
        }
    }

    private fun triggerWakeUp(detectedKeyword: String) {
        val keywordLabel = if (detectedKeyword.isNotBlank()) detectedKeyword else "Hey Arushi"
        Log.i(TAG, "Wake word '$keywordLabel' detected! Waking up Assistant...")

        // Trigger Haptic Feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(140, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(140)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic feedback vibrator failed", e)
        }

        // Send local broadcast to update running MainActivity instantly
        val broadcastIntent = Intent(ACTION_WAKE_WORD_TRIGGERED).apply {
            putExtra(EXTRA_DETECTED_KEYWORD, keywordLabel)
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)

        // Launch MainActivity with trigger_listening set to true
        val wakeIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("trigger_listening", true)
            putExtra(EXTRA_DETECTED_KEYWORD, keywordLabel)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(wakeIntent)
    }

    private fun stopRecordingQuietly() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        }
        audioRecord = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Sam AI Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val prefs = getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        val targetKey = prefs.getString("wake_word_option", WakeWordOption.HEY_ARUSHI.key)
        val option = WakeWordOption.fromKey(targetKey)

        val stopIntent = Intent(this, SamWakeWordService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wake Word Active: '${option.phrase}'")
            .setContentText("Continuous '${option.phrase}' keyword spotter running in background.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Detection", stopPendingIntent)
            .setColor(0xFF1DE9B6.toInt()) // Sam Cyan
            .build()
    }

    override fun onDestroy() {
        Log.i(TAG, "Stopping continuous wake word detection.")
        isListening = false
        stopRecordingQuietly()
        
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // ignore
        }
        releaseWakeLock()

        super.onDestroy()
    }
}
