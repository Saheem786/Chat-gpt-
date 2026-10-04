package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MicrophoneRecorder(
    private val onAudioChunk: (base64Pcm: String, rawBytes: ByteArray) -> Unit,
    private val onAmplitudeChanged: (amplitude: Float) -> Unit,
    private val onInterruptionDetected: () -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "SamMicRecorder"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // ~100ms chunks: 1600 samples * 2 bytes = 3200 bytes
        private const val CHUNK_SIZE_BYTES = 3200
        private const val INTERRUPTION_RMS_THRESHOLD = 0.18f
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    @Volatile
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (isRecording) {
            Log.w(TAG, "Microphone is already recording")
            return
        }

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                onError("Invalid audio record parameters on this device")
                return
            }

            val bufferSize = maxOf(minBufferSize * 2, CHUNK_SIZE_BYTES * 2)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to MIC if VOICE_RECOGNITION fails on some emulators
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Failed to initialize AudioRecord. Check microphone permissions.")
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            Log.i(TAG, "Microphone started recording at ${SAMPLE_RATE}Hz mono 16-bit PCM")

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(CHUNK_SIZE_BYTES)
                var speechFramesInSpeakingState = 0

                while (isActive && isRecording) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        val chunkData = buffer.copyOf(readBytes)

                        // Calculate RMS Amplitude
                        var sum = 0.0
                        val sampleCount = readBytes / 2
                        for (i in 0 until sampleCount) {
                            val sample = (chunkData[i * 2 + 1].toInt() shl 8) or (chunkData[i * 2].toInt() and 0xFF)
                            sum += sample.toDouble() * sample.toDouble()
                        }
                        val rms = if (sampleCount > 0) sqrt(sum / sampleCount) / 32768.0 else 0.0
                        val normalizedAmp = (rms * 3.5).coerceIn(0.0, 1.0).toFloat()

                        onAmplitudeChanged(normalizedAmp)

                        // Check for user speaking interruption
                        if (normalizedAmp > INTERRUPTION_RMS_THRESHOLD) {
                            speechFramesInSpeakingState++
                            if (speechFramesInSpeakingState >= 2) {
                                onInterruptionDetected()
                            }
                        } else {
                            speechFramesInSpeakingState = 0
                        }

                        // Encode to Base64 for Gemini Live
                        val base64 = Base64.encodeToString(chunkData, Base64.NO_WRAP)
                        onAudioChunk(base64, chunkData)
                    } else if (readBytes < 0) {
                        Log.e(TAG, "AudioRecord error reading buffer: $readBytes")
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission denied", e)
            onError("Microphone permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting microphone", e)
            onError("Microphone error: ${e.message}")
        }
    }

    fun stop() {
        Log.i(TAG, "Microphone stopped")
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null
        onAmplitudeChanged(0f)
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
