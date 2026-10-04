package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AudioTrackPlayer(
    private val onPlaybackStarted: () -> Unit,
    private val onPlaybackEnded: () -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit,
    private val onDiagnosticLog: (String) -> Unit
) {
    companion object {
        private const val TAG = "SamAudioPlayer"
        const val DEFAULT_SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var currentSampleRate = DEFAULT_SAMPLE_RATE
    private var playbackJob: Job? = null
    private val audioChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val isPlaying = AtomicBoolean(false)
    private var chunkCount = 0

    init {
        initAudioTrack(DEFAULT_SAMPLE_RATE)
    }

    private fun initAudioTrack(sampleRate: Int) {
        try {
            audioTrack?.release()
            currentSampleRate = sampleRate

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            val bufferSize = maxOf(minBufferSize * 4, 16384)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(sampleRate)
                .setChannelMask(CHANNEL_CONFIG)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            onDiagnosticLog("AudioTrack initialized at ${sampleRate}Hz mono 16-bit PCM (buffer: $bufferSize bytes)")
            Log.i(TAG, "AudioTrack initialized: sampleRate=$sampleRate, state=${audioTrack?.state}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
            onDiagnosticLog("AudioTrack init error: ${e.message}")
        }
    }

    fun startPlaybackLoop(scope: CoroutineScope) {
        if (playbackJob != null) return

        playbackJob = scope.launch(Dispatchers.IO) {
            onDiagnosticLog("Audio playback loop started")
            while (isActive) {
                val pcmData = audioChannel.receive()

                if (!isPlaying.get()) {
                    isPlaying.set(true)
                    try {
                        if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack?.play()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting AudioTrack playback", e)
                    }
                    onPlaybackStarted()
                    onDiagnosticLog("Audio playback started")
                }

                // Calculate amplitude of playing chunk for UI visualization
                val sampleCount = pcmData.size / 2
                var sum = 0.0
                for (i in 0 until sampleCount) {
                    val sample = (pcmData[i * 2 + 1].toInt() shl 8) or (pcmData[i * 2].toInt() and 0xFF)
                    sum += sample.toDouble() * sample.toDouble()
                }
                val rms = if (sampleCount > 0) sqrt(sum / sampleCount) / 32768.0 else 0.0
                val amp = (rms * 4.0).coerceIn(0.0, 1.0).toFloat()
                onAmplitudeChanged(amp)

                // Write PCM samples to AudioTrack
                var bytesWritten = 0
                while (bytesWritten < pcmData.size && isActive) {
                    val written = audioTrack?.write(pcmData, bytesWritten, pcmData.size - bytesWritten) ?: -1
                    if (written > 0) {
                        bytesWritten += written
                    } else {
                        Log.e(TAG, "AudioTrack.write error code: $written")
                        break
                    }
                }

                // If channel is empty after this chunk, brief settle then notify playback ended
                if (audioChannel.isEmpty) {
                    kotlinx.coroutines.delay(120)
                    if (audioChannel.isEmpty && isPlaying.get()) {
                        isPlaying.set(false)
                        onAmplitudeChanged(0f)
                        onPlaybackEnded()
                        onDiagnosticLog("Audio playback ended (completed all queued chunks)")
                    }
                }
            }
        }
    }

    /**
     * Enqueue a PCM chunk from Gemini Live
     */
    fun enqueueChunk(base64Data: String, mimeType: String? = null) {
        try {
            // Check sample rate from MIME type if provided e.g. "audio/pcm;rate=24000"
            mimeType?.let { mime ->
                val rateMatch = Regex("rate=(\\d+)").find(mime)
                if (rateMatch != null) {
                    val rate = rateMatch.groupValues[1].toIntOrNull()
                    if (rate != null && rate != currentSampleRate && rate in 8000..48000) {
                        onDiagnosticLog("Sample rate updated from MIME: ${rate}Hz")
                        initAudioTrack(rate)
                    }
                }
            }

            val pcmBytes = Base64.decode(base64Data, Base64.DEFAULT)
            chunkCount++
            onDiagnosticLog("PCM chunk #$chunkCount decoded: ${pcmBytes.size} bytes (${mimeType ?: "audio/pcm"})")
            audioChannel.trySend(pcmBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode/enqueue PCM chunk", e)
            onDiagnosticLog("PCM decode error: ${e.message}")
        }
    }

    /**
     * Interrupt current playback immediately
     */
    fun interrupt() {
        if (!isPlaying.get() && audioChannel.isEmpty) return

        onDiagnosticLog("Audio playback interrupted by user")
        Log.i(TAG, "Interrupting audio playback")

        // Drain pending chunks
        while (audioChannel.tryReceive().isSuccess) {
            // drop queued chunks
        }

        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing/flushing AudioTrack", e)
        }

        isPlaying.set(false)
        onAmplitudeChanged(0f)
        onPlaybackEnded()
    }

    /**
     * Diagnostic Test Speaker: Plays 440Hz Sine Tone for 1.5 seconds through the exact same output pipeline
     */
    fun testSpeakerTone(scope: CoroutineScope, frequencyHz: Float = 440f, durationMs: Int = 1500) {
        scope.launch(Dispatchers.IO) {
            onDiagnosticLog("Diagnostic Test: Generating 440Hz sine wave tone...")
            interrupt()

            val sampleRate = currentSampleRate
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val pcmData = ByteArray(numSamples * 2)

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                val angle = 2.0 * PI * frequencyHz * time
                // Envelope windowing (fade-in, fade-out to prevent clicks)
                val envelope = when {
                    i < 800 -> i / 800.0
                    i > numSamples - 800 -> (numSamples - i) / 800.0
                    else -> 1.0
                }
                val sampleValue = (sin(angle) * 26000 * envelope).toInt().toShort()

                // Little-endian 16-bit PCM
                pcmData[i * 2] = (sampleValue.toInt() and 0xFF).toByte()
                pcmData[i * 2 + 1] = ((sampleValue.toInt() shr 8) and 0xFF).toByte()
            }

            onDiagnosticLog("Diagnostic Test: 440Hz tone generated (${pcmData.size} bytes). Sending to speaker...")
            audioChannel.send(pcmData)
        }
    }

    fun isAudioPlaying(): Boolean = isPlaying.get()

    fun release() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        }
        audioTrack = null
    }
}
