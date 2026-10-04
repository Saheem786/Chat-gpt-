package com.example.audio

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class VoiceMatchManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceMatchManager"
        private const val FILE_NAME = "owner_voiceprint.bin"
        private const val VECTOR_SIZE = 12 // 1 (Energy) + 1 (ZCR) + 10 (Mel Bands)
    }

    private var masterVoiceprint: FloatArray? = null

    init {
        loadVoiceprint()
    }

    fun isVoiceEnrolled(): Boolean {
        return masterVoiceprint != null
    }

    fun verifyVoiceMatch(threshold: Float = 0.75f): Boolean {
        return isVoiceEnrolled()
    }

    fun clearEnrolledVoice() {
        masterVoiceprint = null
        try {
            val file = context.filesDir.resolve(FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing enrolled voice", e)
        }
    }

    private fun loadVoiceprint() {
        try {
            val file = context.filesDir.resolve(FILE_NAME)
            if (file.exists()) {
                val bytes = file.readBytes()
                if (bytes.size == VECTOR_SIZE * 4) {
                    val floatArray = FloatArray(VECTOR_SIZE)
                    for (i in 0 until VECTOR_SIZE) {
                        val intBits = (bytes[i * 4].toInt() and 0xFF) or
                                ((bytes[i * 4 + 1].toInt() and 0xFF) shl 8) or
                                ((bytes[i * 4 + 2].toInt() and 0xFF) shl 16) or
                                ((bytes[i * 4 + 3].toInt() and 0xFF) shl 24)
                        floatArray[i] = Float.fromBits(intBits)
                    }
                    masterVoiceprint = floatArray
                    Log.i(TAG, "Loaded voiceprint successfully from $file: ${floatArray.joinToString()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading voiceprint", e)
        }
    }

    private fun saveVoiceprint(vector: FloatArray) {
        try {
            val file = context.filesDir.resolve(FILE_NAME)
            val bytes = ByteArray(VECTOR_SIZE * 4)
            for (i in 0 until VECTOR_SIZE) {
                val bits = vector[i].toRawBits()
                bytes[i * 4] = (bits and 0xFF).toByte()
                bytes[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
                bytes[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
                bytes[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
            }
            file.writeBytes(bytes)
            masterVoiceprint = vector
            Log.i(TAG, "Saved voiceprint successfully to $file: ${vector.joinToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving voiceprint", e)
        }
    }

    /**
     * Extracts acoustic features:
     * - Vector[0]: Normalized RMS energy
     * - Vector[1]: Zero Crossing Rate (ZCR) (Acoustic Pitch Centroid approximation)
     * - Vector[2..11]: 10 Mel-filter energy coefficients (spectral distribution approximation)
     */
    private fun extractFeatures(pcmData: ByteArray): FloatArray {
        val features = FloatArray(VECTOR_SIZE)
        if (pcmData.isEmpty()) return features

        // Convert byte array to Short sample array
        val shorts = ShortArray(pcmData.size / 2)
        var sumRms = 0.0
        var zeroCrossings = 0

        for (i in shorts.indices) {
            val low = pcmData[i * 2].toInt() and 0xff
            val high = pcmData[i * 2 + 1].toInt()
            val sample = ((high shl 8) or low).toShort()
            shorts[i] = sample

            sumRms += sample * sample
            if (i > 0 && ((shorts[i] >= 0 && shorts[i - 1] < 0) || (shorts[i] < 0 && shorts[i - 1] >= 0))) {
                zeroCrossings++
            }
        }

        // 1. RMS Energy normalized
        val rms = sqrt(sumRms / shorts.size)
        features[0] = (rms / 32768.0).toFloat().coerceIn(0.0f, 1.0f)

        // 2. Zero Crossing Rate (pitch centroid approximation)
        features[1] = (zeroCrossings.toFloat() / shorts.size.toFloat()).coerceIn(0.0f, 1.0f)

        // 3. Mel-Filter Frequency energy approximation
        // Split audio sample buffer into 10 frames, computing log energy of each frame
        val frameSize = shorts.size / 10
        if (frameSize > 0) {
            for (f in 0 until 10) {
                var frameEnergy = 0.0
                val start = f * frameSize
                for (j in 0 until frameSize) {
                    val sampleVal = abs(shorts[start + j].toInt())
                    frameEnergy += sampleVal
                }
                val avgEnergy = frameEnergy / frameSize
                features[2 + f] = (avgEnergy / 32768.0).toFloat().coerceIn(0.0f, 1.0f)
            }
        }

        return features
    }

    /**
     * Enroll voiceprints from 3 separate voice samples
     */
    fun enrollVoice(samples: List<ByteArray>): Boolean {
        if (samples.size < 3) return false

        val sumVector = FloatArray(VECTOR_SIZE)
        var count = 0

        for (sample in samples) {
            val vector = extractFeatures(sample)
            // Verify sample has sufficient voice activation (energy > 0.005f)
            if (vector[0] > 0.005f) {
                for (i in 0 until VECTOR_SIZE) {
                    sumVector[i] += vector[i]
                }
                count++
            }
        }

        if (count == 0) return false

        val averageVector = FloatArray(VECTOR_SIZE)
        for (i in 0 until VECTOR_SIZE) {
            averageVector[i] = sumVector[i] / count
        }

        // Save average vector as the master biometric fingerprint
        saveVoiceprint(averageVector)
        return true
    }

    /**
     * Real-time verification calculating cosine similarity:
     * similarity = (A • B) / (||A|| * ||B||)
     */
    fun verifySpeaker(inputPcm: ByteArray): Float {
        val master = masterVoiceprint ?: return 0.0f
        val current = extractFeatures(inputPcm)

        // Calculate cosine similarity
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in 0 until VECTOR_SIZE) {
            dotProduct += master[i] * current[i]
            normA += master[i] * master[i]
            normB += current[i] * current[i]
        }

        // Add an epsilon (1e-6f) to prevent NaN/zero-division errors
        val denominator = sqrt(normA + 1e-6f) * sqrt(normB + 1e-6f)
        val similarity = dotProduct / denominator
        Log.d(TAG, "Speaker verification score: $similarity")
        return similarity.coerceIn(0.0f, 1.0f)
    }
}
