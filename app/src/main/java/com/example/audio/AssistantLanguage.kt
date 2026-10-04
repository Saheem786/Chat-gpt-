package com.example.audio

import java.util.Locale

/**
 * Supported spoken and recognition languages for Sam AI / Arushi voice assistant.
 * Dynamically configures Android's SpeechRecognizer and TextToSpeech engine.
 */
enum class AssistantLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val speechLocale: String,
    val ttsLanguage: String,
    val ttsCountry: String,
    val additionalLocales: List<String>,
    val description: String = "",
    val samplePrompt: String = "",
    val accuracyBadge: String = "High Accuracy",
    val isPrimary: Boolean = false
) {
    HINGLISH(
        code = "hinglish",
        displayName = "Hinglish",
        nativeName = "Hindi + English",
        flag = "🇮🇳",
        speechLocale = "hi-IN",
        ttsLanguage = "hi",
        ttsCountry = "IN",
        additionalLocales = listOf("en-IN", "en-US", "hi"),
        description = "Optimized for natural Indian bilingual speech ('Ek joke sunao', 'Alarm set karo')",
        samplePrompt = "Ek romantic gaana chalao",
        accuracyBadge = "Bilingual Code-Switching",
        isPrimary = true
    ),
    HINDI(
        code = "hi_in",
        displayName = "हिन्दी",
        nativeName = "Hindi",
        flag = "🇮🇳",
        speechLocale = "hi-IN",
        ttsLanguage = "hi",
        ttsCountry = "IN",
        additionalLocales = listOf("hi"),
        description = "High-precision pure Devanagari Hindi recognition & authentic Hindi TTS voice",
        samplePrompt = "आज की ताज़ा ख़बरें क्या हैं?",
        accuracyBadge = "Pure Devanagari ASR",
        isPrimary = true
    ),
    ENGLISH_IN(
        code = "en_in",
        displayName = "English (India)",
        nativeName = "Indian English",
        flag = "🇮🇳",
        speechLocale = "en-IN",
        ttsLanguage = "en",
        ttsCountry = "IN",
        additionalLocales = listOf("hi-IN", "en-US"),
        description = "Tuned for Indian English phonetics, accents, and local names recognition",
        samplePrompt = "Set an alarm for 7:30 AM tomorrow",
        accuracyBadge = "Indian Accent Tuned",
        isPrimary = true
    ),
    ENGLISH_US(
        code = "en_us",
        displayName = "English (US)",
        nativeName = "US English",
        flag = "🇺🇸",
        speechLocale = "en-US",
        ttsLanguage = "en",
        ttsCountry = "US",
        additionalLocales = listOf("en-GB", "en-IN"),
        description = "Standard international English speech recognition and synthesis",
        samplePrompt = "What is the capital of France?",
        accuracyBadge = "Standard US English",
        isPrimary = true
    ),
    SPANISH(
        code = "es_es",
        displayName = "Español",
        nativeName = "Spanish",
        flag = "🇪🇸",
        speechLocale = "es-ES",
        ttsLanguage = "es",
        ttsCountry = "ES",
        additionalLocales = listOf("es-US", "es-MX"),
        description = "Reconocimiento y voz en español",
        samplePrompt = "¿Qué tiempo hace hoy?",
        accuracyBadge = "Spanish Model",
        isPrimary = false
    ),
    FRENCH(
        code = "fr_fr",
        displayName = "Français",
        nativeName = "French",
        flag = "🇫🇷",
        speechLocale = "fr-FR",
        ttsLanguage = "fr",
        ttsCountry = "FR",
        additionalLocales = listOf("fr-CA"),
        description = "Reconnaissance vocale en français",
        samplePrompt = "Raconte-moi une histoire",
        accuracyBadge = "French Model",
        isPrimary = false
    ),
    GERMAN(
        code = "de_de",
        displayName = "Deutsch",
        nativeName = "German",
        flag = "🇩🇪",
        speechLocale = "de-DE",
        ttsLanguage = "de",
        ttsCountry = "DE",
        additionalLocales = listOf("de-AT", "de-CH"),
        description = "Spracherkennung auf Deutsch",
        samplePrompt = "Wie spät ist es?",
        accuracyBadge = "German Model",
        isPrimary = false
    ),
    JAPANESE(
        code = "ja_jp",
        displayName = "日本語",
        nativeName = "Japanese",
        flag = "🇯🇵",
        speechLocale = "ja-JP",
        ttsLanguage = "ja",
        ttsCountry = "JP",
        additionalLocales = listOf("ja"),
        description = "日本語音声認識および音声合成",
        samplePrompt = "今日の天気を教えて",
        accuracyBadge = "Japanese Model",
        isPrimary = false
    );

    val ttsLocale: Locale
        get() = Locale.forLanguageTag(speechLocale)

    companion object {
        fun fromCode(code: String?): AssistantLanguage {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINGLISH
        }
    }
}
