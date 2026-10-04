package com.example.audio

import android.util.Log
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Supported Wake Word options for always-on keyword spotter.
 */
enum class WakeWordOption(val key: String, val phrase: String, val displayName: String) {
    HEY_JARVIS("hey_jarvis", "Hey Jarvis", "Hey Jarvis (Recommended)"),
    HEY_ARUSHI("hey_arushi", "Hey Arushi", "Hey Arushi"),
    HEY_SAM("hey_sam", "Hey Sam", "Hey Sam"),
    ANY("any", "Hey Jarvis / Hey Sam", "Any (Jarvis or Sam)");

    companion object {
        fun fromKey(key: String?): WakeWordOption {
            return when (key?.lowercase()?.trim()) {
                "hey_sam", "sam" -> HEY_SAM
                "hey_arushi", "arushi" -> HEY_ARUSHI
                "any", "both" -> ANY
                else -> HEY_JARVIS
            }
        }
    }
}

/**
 * Result returned by the keyword spotter evaluation.
 */
data class KeywordSpotterResult(
    val isMatch: Boolean,
    val detectedKeyword: String,
    val confidence: Float,
    val details: String = ""
)

/**
 * Lightweight, on-device acoustic keyword spotter for hands-free wake word detection
 * (e.g., 'Hey Jarvis', 'Hey Sam').
 *
 * Designed for low power consumption, zero cloud latency, and 100% offline execution.
 * Operates on 16kHz 16-bit Mono PCM audio.
 */
class LightweightKeywordSpotter(
    private var defaultOption: WakeWordOption = WakeWordOption.HEY_JARVIS,
    private var sensitivity: Float = 0.60f // Lower value = higher sensitivity
) {

    companion object {
        private const val TAG = "KeywordSpotter"
        const val SAMPLE_RATE = 16000
        private const val FRAME_SIZE_MS = 25
        private const val HOP_SIZE_MS = 10
        private const val SAMPLES_PER_FRAME = (SAMPLE_RATE * FRAME_SIZE_MS) / 1000 // 400 samples
        private const val SAMPLES_PER_HOP = (SAMPLE_RATE * HOP_SIZE_MS) / 1000 // 160 samples
    }

    /**
     * Acoustic Frame summary
     */
    data class AcousticFrame(
        val rmsEnergy: Float,
        val zeroCrossingRate: Float,
        val lowBandEnergyRatio: Float,  // 100Hz - 900Hz (Vowels)
        val midBandEnergyRatio: Float,  // 900Hz - 2800Hz (Formants / Liquids)
        val highBandEnergyRatio: Float  // 2800Hz - 8000Hz (Sibilants / Fricatives)
    )

    fun setWakeWordOption(option: WakeWordOption) {
        defaultOption = option
    }

    fun setSensitivity(sens: Float) {
        sensitivity = sens.coerceIn(0.40f, 0.85f)
    }

    /**
     * Evaluates a PCM 16kHz 16-bit mono audio window for keyword activation.
     */
    fun evaluatePcm(
        pcmData: ByteArray,
        targetOption: WakeWordOption = defaultOption,
        threshold: Float = sensitivity
    ): KeywordSpotterResult {
        if (pcmData.size < SAMPLE_RATE * 0.40 * 2) {
            // Less than 400ms of audio, not enough for wake words
            return KeywordSpotterResult(false, "", 0.0f, "Audio segment too short (<400ms)")
        }

        // Convert byte buffer to Short samples
        val samples = ShortArray(pcmData.size / 2)
        var maxAmplitude = 0
        for (i in samples.indices) {
            val low = pcmData[i * 2].toInt() and 0xFF
            val high = pcmData[i * 2 + 1].toInt()
            val sample = (high shl 8) or low
            samples[i] = sample.toShort()
            val absSample = abs(sample)
            if (absSample > maxAmplitude) {
                maxAmplitude = absSample
            }
        }

        // Quick VAD rejection if silence or low ambient noise
        if (maxAmplitude < 600) {
            return KeywordSpotterResult(false, "", 0.0f, "Signal energy below speech threshold")
        }

        val frames = extractAcousticFrames(samples)
        if (frames.size < 15) {
            return KeywordSpotterResult(false, "", 0.0f, "Insufficient voiced frames")
        }

        return when (targetOption) {
            WakeWordOption.HEY_JARVIS -> scoreJarvisSequence(frames, threshold)
            WakeWordOption.HEY_ARUSHI -> scoreArushiSequence(frames, threshold)
            WakeWordOption.HEY_SAM -> scoreSamSequence(frames, threshold)
            WakeWordOption.ANY -> {
                val jarvisRes = scoreJarvisSequence(frames, threshold)
                val samRes = scoreSamSequence(frames, threshold)
                if (jarvisRes.confidence >= samRes.confidence && jarvisRes.isMatch) {
                    jarvisRes
                } else if (samRes.isMatch) {
                    samRes
                } else if (jarvisRes.confidence >= samRes.confidence) {
                    jarvisRes
                } else {
                    samRes
                }
            }
        }
    }

    /**
     * Breaks samples into overlapping frames and extracts normalized acoustic features.
     */
    private fun extractAcousticFrames(samples: ShortArray): List<AcousticFrame> {
        val frames = mutableListOf<AcousticFrame>()
        var offset = 0
        while (offset + SAMPLES_PER_FRAME <= samples.size) {
            var sumSquare = 0.0
            var zeroCrossings = 0
            var prevSample = samples[offset].toInt()

            var lowEnergy = 0.0
            var midEnergy = 0.0
            var highEnergy = 0.0

            for (i in 0 until SAMPLES_PER_FRAME) {
                val current = samples[offset + i].toInt()
                sumSquare += (current * current)

                if ((current >= 0 && prevSample < 0) || (current < 0 && prevSample >= 0)) {
                    zeroCrossings++
                }
                prevSample = current

                // Approximate spectral distribution using simple FIR frequency weighting
                val sampleFloat = current / 32768.0f
                val absVal = abs(sampleFloat)
                // Low-pass approximation (moving difference)
                val diff = if (i > 0) abs(current - samples[offset + i - 1]) / 32768.0f else 0f
                if (diff > 0.08f) {
                    highEnergy += diff
                } else if (diff > 0.02f) {
                    midEnergy += diff
                } else {
                    lowEnergy += absVal
                }
            }

            val rms = sqrt(sumSquare / SAMPLES_PER_FRAME).toFloat() / 32768.0f
            val zcr = zeroCrossings.toFloat() / SAMPLES_PER_FRAME

            val totalEnergy = (lowEnergy + midEnergy + highEnergy).coerceAtLeast(1e-6)
            frames.add(
                AcousticFrame(
                    rmsEnergy = rms,
                    zeroCrossingRate = zcr,
                    lowBandEnergyRatio = (lowEnergy / totalEnergy).toFloat(),
                    midBandEnergyRatio = (midEnergy / totalEnergy).toFloat(),
                    highBandEnergyRatio = (highEnergy / totalEnergy).toFloat()
                )
            )

            offset += SAMPLES_PER_HOP
        }
        return frames
    }

    /**
     * Spots "Hey Arushi" / "Arushi":
     * Phonetics:
     * 1. [Hey/A] (Voiced vowel, low ZCR 0.02 - 0.09, dominant low/mid frequency)
     * 2. [ru] (Liquid transition, mid-band formant energy)
     * 3. [shi] (Sharp sibilant fricative /ʃ/, high ZCR > 0.13, dominant high band energy)
     */
    private fun scoreArushiSequence(frames: List<AcousticFrame>, threshold: Float): KeywordSpotterResult {
        var hasVoicedVowel = false
        var hasLiquidTransition = false
        var hasSibilantBurst = false
        var sibilantPeakZcr = 0.0f
        var vowelCount = 0
        var sibilantCount = 0

        // Traverse temporal sequence
        for (i in frames.indices) {
            val f = frames[i]

            // Check for voiced vowel onset ("Hey" / "A-")
            if (f.rmsEnergy > 0.02f && f.zeroCrossingRate in 0.015f..0.11f && f.lowBandEnergyRatio > 0.25f) {
                vowelCount++
                if (vowelCount >= 3) {
                    hasVoicedVowel = true
                }
            }

            // Check for liquid transition ("ru")
            if (hasVoicedVowel && f.rmsEnergy > 0.018f && f.midBandEnergyRatio > 0.22f) {
                hasLiquidTransition = true
            }

            // Check for terminal sibilant fricative burst ("shi")
            if (hasVoicedVowel && f.zeroCrossingRate > 0.12f && (f.highBandEnergyRatio > 0.25f || f.zeroCrossingRate > 0.15f)) {
                sibilantCount++
                if (f.zeroCrossingRate > sibilantPeakZcr) {
                    sibilantPeakZcr = f.zeroCrossingRate
                }
                if (sibilantCount >= 2) {
                    hasSibilantBurst = true
                }
            }
        }

        // Calculate confidence score based on phonetic cadence match
        var confidence = 0.0f
        if (hasVoicedVowel) confidence += 0.35f
        if (hasLiquidTransition) confidence += 0.25f
        if (hasSibilantBurst) {
            confidence += 0.40f
            // Bonus for crisp fricative profile matching /ʃ/
            if (sibilantPeakZcr > 0.18f) confidence += 0.05f
        }

        confidence = confidence.coerceIn(0.0f, 1.0f)
        val isMatch = confidence >= threshold && hasVoicedVowel && hasSibilantBurst

        return KeywordSpotterResult(
            isMatch = isMatch,
            detectedKeyword = "Hey Arushi",
            confidence = confidence,
            details = "Arushi Cadence: vowel=$hasVoicedVowel, liquid=$hasLiquidTransition, sibilant=$hasSibilantBurst, peakZcr=$sibilantPeakZcr"
        )
    }

    /**
     * Spots "Hey Sam" / "Sam":
     * Phonetics:
     * 1. [S] (Sharp alveolar sibilant /s/, high ZCR > 0.15, high band energy)
     * 2. [am] (Vowel-nasal decay, low ZCR < 0.08, low band energy)
     */
    private fun scoreSamSequence(frames: List<AcousticFrame>, threshold: Float): KeywordSpotterResult {
        var hasSibilantOnset = false
        var hasVowelNasalDecay = false
        var sibilantFrames = 0
        var decayFrames = 0
        var peakZcr = 0f

        for (i in frames.indices) {
            val f = frames[i]

            // Sibilant check (/s/)
            if (f.zeroCrossingRate > 0.13f && f.rmsEnergy > 0.015f) {
                sibilantFrames++
                if (f.zeroCrossingRate > peakZcr) peakZcr = f.zeroCrossingRate
                if (sibilantFrames >= 2) {
                    hasSibilantOnset = true
                }
            }

            // Vowel nasal decay (/am/)
            if (hasSibilantOnset && f.zeroCrossingRate < 0.09f && f.rmsEnergy > 0.015f) {
                decayFrames++
                if (decayFrames >= 2) {
                    hasVowelNasalDecay = true
                }
            }
        }

        var confidence = 0.0f
        if (hasSibilantOnset) confidence += 0.45f
        if (hasVowelNasalDecay) confidence += 0.45f
        if (peakZcr > 0.18f) confidence += 0.10f

        confidence = confidence.coerceIn(0.0f, 1.0f)
        val isMatch = confidence >= threshold && hasSibilantOnset && hasVowelNasalDecay

        return KeywordSpotterResult(
            isMatch = isMatch,
            detectedKeyword = "Hey Sam",
            confidence = confidence,
            details = "Sam Cadence: sibilant=$hasSibilantOnset, nasalDecay=$hasVowelNasalDecay, peakZcr=$peakZcr"
        )
    }

    /**
     * Spots "Hey Jarvis" / "Jarvis":
     * Phonetics:
     * 1. [J] (Affricate, moderate ZCR 0.07 - 0.13, low-mid energy)
     * 2. [ar] (Vowel-rhotic transition, low ZCR < 0.07, high energy)
     * 3. [vis] (Sibilant/fricative tail, high ZCR > 0.13)
     */
    private fun scoreJarvisSequence(frames: List<AcousticFrame>, threshold: Float): KeywordSpotterResult {
        var hasAffricateOnset = false
        var hasVowelCore = false
        var hasSibilantTail = false
        var peakZcr = 0f

        for (i in frames.indices) {
            val f = frames[i]

            // Affricate onset check ([J])
            if (!hasAffricateOnset && f.zeroCrossingRate in 0.07f..0.13f && f.rmsEnergy > 0.012f) {
                hasAffricateOnset = true
            }

            // Vowel core check ([ar])
            if (hasAffricateOnset && f.zeroCrossingRate < 0.07f && f.rmsEnergy > 0.018f) {
                hasVowelCore = true
            }

            // Sibilant tail check ([vis])
            if (hasVowelCore && f.zeroCrossingRate > 0.13f && f.rmsEnergy > 0.012f) {
                hasSibilantTail = true
                if (f.zeroCrossingRate > peakZcr) peakZcr = f.zeroCrossingRate
            }
        }

        var confidence = 0.0f
        if (hasAffricateOnset) confidence += 0.35f
        if (hasVowelCore) confidence += 0.35f
        if (hasSibilantTail) confidence += 0.30f

        confidence = confidence.coerceIn(0.0f, 1.0f)
        val isMatch = confidence >= threshold && hasVowelCore && hasSibilantTail

        return KeywordSpotterResult(
            isMatch = isMatch,
            detectedKeyword = "Hey Jarvis",
            confidence = confidence,
            details = "Jarvis Cadence: onset=$hasAffricateOnset, vowel=$hasVowelCore, tail=$hasSibilantTail, peakZcr=$peakZcr"
        )
    }

    /**
     * Synthesizes synthetic phonetic PCM waveforms matching "Hey Arushi" or "Hey Sam"
     * for JVM Robolectric unit testing and calibration.
     */
    fun synthesizeKeywordPcm(keyword: String, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val durationSec = 1.0
        val totalSamples = (sampleRate * durationSec).toInt()
        val pcm = ByteArray(totalSamples * 2)

        val isArushi = keyword.contains("arushi", ignoreCase = true)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal: Short = if (isArushi) {
                // "Hey Arushi":
                // 0.0 to 0.35s: "Hey" / "A" (Vowels ~280Hz + 600Hz)
                // 0.35 to 0.60s: "ru" (Liquid ~400Hz + 1400Hz)
                // 0.60 to 0.95s: "shi" (High frequency fricative noise ~4500Hz with high ZCR)
                when {
                    t < 0.35 -> {
                        val env = sin(Math.PI * (t / 0.35))
                        ((sin(2 * Math.PI * 320 * t) + 0.5 * sin(2 * Math.PI * 640 * t)) * env * 14000).toInt().toShort()
                    }
                    t < 0.60 -> {
                        val env = sin(Math.PI * ((t - 0.35) / 0.25))
                        ((sin(2 * Math.PI * 450 * t) + 0.7 * sin(2 * Math.PI * 1500 * t)) * env * 12000).toInt().toShort()
                    }
                    else -> {
                        val env = sin(Math.PI * ((t - 0.60) / 0.35))
                        // High frequency oscillation mimicking /ʃ/ sibilance
                        ((sin(2 * Math.PI * 4200 * t) + (if (i % 2 == 0) 0.6 else -0.6)) * env * 13000).toInt().toShort()
                    }
                }
            } else {
                // "Hey Sam":
                // 0.0 to 0.30s: "Hey"
                // 0.30 to 0.60s: "S" (high ZCR fricative noise ~4800Hz)
                // 0.60 to 0.95s: "am" (low vowel + nasal ~240Hz)
                when {
                    t < 0.30 -> {
                        val env = sin(Math.PI * (t / 0.30))
                        (sin(2 * Math.PI * 300 * t) * env * 12000).toInt().toShort()
                    }
                    t < 0.60 -> {
                        val env = sin(Math.PI * ((t - 0.30) / 0.30))
                        ((sin(2 * Math.PI * 4600 * t) + (if (i % 2 == 0) 0.5 else -0.5)) * env * 13000).toInt().toShort()
                    }
                    else -> {
                        val env = sin(Math.PI * ((t - 0.60) / 0.35))
                        (sin(2 * Math.PI * 220 * t) * env * 11000).toInt().toShort()
                    }
                }
            }

            pcm[i * 2] = (sampleVal.toInt() and 0xFF).toByte()
            pcm[i * 2 + 1] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }

        return pcm
    }
}
