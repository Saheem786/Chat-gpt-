package com.example.system

import android.util.Log

object ConversationalParser {
    private const val TAG = "ConversationalParser"

    fun processCorrections(input: String): String {
        val lower = input.lowercase()
        val correctionCues = listOf("nahi", "sorry", "actually", "wait", "instead")
        var containsCue = false
        for (cue in correctionCues) {
            if (lower.contains(cue)) {
                containsCue = true
                break
            }
        }

        if (!containsCue) return input

        Log.d(TAG, "Correction marker detected inside user input: '$input'")

        val regexSplit = Regex("(?i)\\s+(?:nahi|sorry|actually|wait|instead)\\s+")
        val segments = input.split(regexSplit)

        if (segments.size >= 2) {
            val firstSegment = segments[0].trim()
            val secondSegment = segments[1].trim()

            val timeKeywords = listOf("baje", "alarm", "wifi", "torch", "volume", "baji")
            var carryOverPrefix = ""
            val firstSegmentWords = firstSegment.split(" ")
            if (firstSegmentWords.size > 1) {
                val prefixWords = mutableListOf<String>()
                for (word in firstSegmentWords) {
                    val isNumOrTime = word.any { it.isDigit() } || word.lowercase() in timeKeywords
                    if (isNumOrTime) {
                        break
                    }
                    prefixWords.add(word)
                }
                carryOverPrefix = prefixWords.joinToString(" ")
            }

            val result = if (carryOverPrefix.isNotBlank() && !secondSegment.lowercase().contains(carryOverPrefix.lowercase())) {
                "$carryOverPrefix $secondSegment"
            } else {
                secondSegment
            }
            Log.i(TAG, "Reconstructed corrected sentence: '$result'")
            return result
        }

        return input
    }
}
