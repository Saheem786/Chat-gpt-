package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gemini.SamPersonality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS AI", appName)
    }

    @Test
    fun `verify sam personality configuration`() {
        assertEquals("JARVIS", SamPersonality.ASSISTANT_NAME)
        assertEquals("Fenrir", SamPersonality.VOICE_NAME)
        assertEquals("models/gemini-3.8-flash", SamPersonality.MODEL_LIVE_PRIMARY)
        assertNotNull(SamPersonality.SYSTEM_INSTRUCTION)
    }

    @Test
    fun `verify room chat repository save and retrieval`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.data.ChatRepository(db.chatMessageDao())

        val msg = com.example.ui.ChatMessage(
            sender = com.example.ui.MessageSender.USER,
            text = "Testing Room chat history persistence"
        )
        repo.saveMessage(msg)

        val count = repo.getMessageCount()
        assertEquals(1, count)

        repo.clearHistory()
        assertEquals(0, repo.getMessageCount())
        db.close()
    }

    @Test
    fun `verify text to speech engine initialization and lifecycle`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = com.example.audio.TextToSpeechEngine(
            context = context,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        )
        assertNotNull(engine)
        engine.speak("Hello from JARVIS")
        engine.stop()
        engine.release()
    }

    @Test
    fun `verify device action manager app launcher and fallback`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.device.DeviceActionManager(context)
        val youtubeSuccess = manager.launchApp("YouTube")
        org.junit.Assert.assertTrue(youtubeSuccess)

        val whatsappSuccess = manager.launchApp("WhatsApp")
        org.junit.Assert.assertTrue(whatsappSuccess)
    }

    @Test
    fun `verify assistant language enum and dynamic speech configuration`() {
        val hinglish = com.example.audio.AssistantLanguage.HINGLISH
        assertEquals("hi-IN", hinglish.speechLocale)
        assertEquals("hinglish", hinglish.code)
        org.junit.Assert.assertTrue(hinglish.additionalLocales.contains("en-IN"))

        val englishUs = com.example.audio.AssistantLanguage.ENGLISH_US
        assertEquals("en-US", englishUs.speechLocale)

        val hindi = com.example.audio.AssistantLanguage.HINDI
        assertEquals("hi-IN", hindi.speechLocale)

        val englishIn = com.example.audio.AssistantLanguage.ENGLISH_IN
        assertEquals("en-IN", englishIn.speechLocale)

        val resolved = com.example.audio.AssistantLanguage.fromCode("es_es")
        assertEquals(com.example.audio.AssistantLanguage.SPANISH, resolved)

        val resolvedHindi = com.example.audio.AssistantLanguage.fromCode("hi_in")
        assertEquals(com.example.audio.AssistantLanguage.HINDI, resolvedHindi)

        // Verify dynamic instruction adaptation in SamPersonality
        com.example.gemini.SamPersonality.updateLanguage(com.example.audio.AssistantLanguage.HINDI)
        org.junit.Assert.assertTrue(com.example.gemini.SamPersonality.SYSTEM_INSTRUCTION.contains("Devanagari Hindi"))

        com.example.gemini.SamPersonality.updateLanguage(com.example.audio.AssistantLanguage.HINGLISH)
        org.junit.Assert.assertTrue(com.example.gemini.SamPersonality.SYSTEM_INSTRUCTION.contains("Conversational Hinglish"))
    }

    @Test
    fun `verify speech recognizer intent dynamic configuration for hinglish hindi and english`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handler = com.example.audio.VoiceInputHandler(context)

        // Hinglish Intent Verification
        val hinglishIntent = handler.buildRecognizerIntent(com.example.audio.AssistantLanguage.HINGLISH)
        assertEquals("hi-IN", hinglishIntent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
        val hinglishAdditional = hinglishIntent.getStringArrayExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES")
        assertNotNull(hinglishAdditional)
        org.junit.Assert.assertTrue(hinglishAdditional!!.contains("en-IN"))
        org.junit.Assert.assertTrue(hinglishAdditional.contains("hi-IN"))

        // Hindi Intent Verification
        val hindiIntent = handler.buildRecognizerIntent(com.example.audio.AssistantLanguage.HINDI)
        assertEquals("hi-IN", hindiIntent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
        org.junit.Assert.assertTrue(hindiIntent.getBooleanExtra(android.speech.RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false))

        // English (India) Intent Verification
        val englishInIntent = handler.buildRecognizerIntent(com.example.audio.AssistantLanguage.ENGLISH_IN)
        assertEquals("en-IN", englishInIntent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))

        // English (US) Intent Verification
        val englishUsIntent = handler.buildRecognizerIntent(com.example.audio.AssistantLanguage.ENGLISH_US)
        assertEquals("en-US", englishUsIntent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
    }

    @Test
    fun `verify ai engine mode quick response and deep thought configurations`() {
        val quick = com.example.gemini.AiEngineMode.QUICK
        assertEquals("gemini-3.8-flash", quick.modelName)
        assertEquals(0.4, quick.temperature, 0.001)
        assertEquals(150, quick.maxOutputTokens)
        assertEquals(20, quick.topK)
        assertEquals("⚡", quick.icon)

        val deep = com.example.gemini.AiEngineMode.DEEP_THINK
        assertEquals("gemini-3.1-pro-preview", deep.modelName)
        assertEquals(0.7, deep.temperature, 0.001)
        assertEquals(4096, deep.maxOutputTokens)
        assertEquals("🧠", deep.icon)

        assertEquals(com.example.gemini.AiEngineMode.QUICK, com.example.gemini.AiEngineMode.fromName("QUICK"))
        assertEquals(com.example.gemini.AiEngineMode.DEEP_THINK, com.example.gemini.AiEngineMode.fromName("DEEP_THINK"))
        assertEquals(com.example.gemini.AiEngineMode.DEEP_THINK, com.example.gemini.AiEngineMode.fromName("thinking"))
    }

    @Test
    fun `verify lightweight keyword spotter for hey arushi and hey sam wake words`() {
        val spotter = com.example.audio.LightweightKeywordSpotter()

        // 1. Verify WakeWordOption enum mappings
        val jarvisOption = com.example.audio.WakeWordOption.HEY_JARVIS
        assertEquals("hey_jarvis", jarvisOption.key)
        assertEquals("Hey Jarvis", jarvisOption.phrase)
        assertEquals(com.example.audio.WakeWordOption.HEY_JARVIS, com.example.audio.WakeWordOption.fromKey("hey_jarvis"))
        assertEquals(com.example.audio.WakeWordOption.HEY_SAM, com.example.audio.WakeWordOption.fromKey("hey_sam"))
        assertEquals(com.example.audio.WakeWordOption.ANY, com.example.audio.WakeWordOption.fromKey("any"))

        // 2. Synthesize audio waveform matching "Hey Arushi" acoustic phonetic profile
        val arushiPcm = spotter.synthesizeKeywordPcm("Hey Arushi")
        org.junit.Assert.assertTrue(arushiPcm.isNotEmpty())

        val arushiResult = spotter.evaluatePcm(arushiPcm, com.example.audio.WakeWordOption.HEY_ARUSHI, threshold = 0.50f)
        org.junit.Assert.assertTrue("Arushi result should match", arushiResult.isMatch)
        assertEquals("Hey Arushi", arushiResult.detectedKeyword)
        org.junit.Assert.assertTrue("Arushi confidence should exceed 0.5", arushiResult.confidence >= 0.50f)

        // 3. Synthesize audio waveform matching "Hey Sam" acoustic phonetic profile
        val samPcm = spotter.synthesizeKeywordPcm("Hey Sam")
        val samResult = spotter.evaluatePcm(samPcm, com.example.audio.WakeWordOption.HEY_SAM, threshold = 0.50f)
        org.junit.Assert.assertTrue("Sam result should match", samResult.isMatch)
        assertEquals("Hey Sam", samResult.detectedKeyword)
        org.junit.Assert.assertTrue("Sam confidence should exceed 0.5", samResult.confidence >= 0.50f)

        // 4. Verify rejection of silence / sub-threshold noise
        val silencePcm = ByteArray(16000 * 2) // 1 second of zero bytes
        val silenceResult = spotter.evaluatePcm(silencePcm, com.example.audio.WakeWordOption.HEY_JARVIS)
        org.junit.Assert.assertFalse("Silence should not trigger wake word", silenceResult.isMatch)
        assertEquals(0.0f, silenceResult.confidence, 0.001f)

        // 5. Verify broadcast action constant
        assertEquals("com.example.sam.ACTION_WAKE_WORD_TRIGGERED", com.example.audio.SamWakeWordService.ACTION_WAKE_WORD_TRIGGERED)
    }

    @Test
    fun `verify human companion persona instructions and conversational rules`() {
        com.example.gemini.SamPersonality.updatePersona("SAM")
        val instruction = com.example.gemini.SamPersonality.SYSTEM_INSTRUCTION
        org.junit.Assert.assertTrue(
            "Should state Sam is NOT an AI or chatbot",
            instruction.contains("You are Sam. You are NOT an AI, a chatbot, or a virtual assistant.")
        )
        org.junit.Assert.assertTrue(
            "Should include smart chill close friend persona",
            instruction.contains("smart, chill, dependable close friend")
        )
        org.junit.Assert.assertTrue(
            "Should include CONVERSATIONAL RULES (STRICT)",
            instruction.contains("CONVERSATIONAL RULES (STRICT):")
        )
        org.junit.Assert.assertTrue(
            "Should forbid robotic introductions",
            instruction.contains("Never introduce yourself as an AI, language model, or virtual assistant.")
        )
        org.junit.Assert.assertTrue(
            "Should mandate authentic Hinglish vocabulary",
            instruction.contains("Tone & Vocabulary: Speak natural, authentic everyday Hinglish")
        )
        org.junit.Assert.assertTrue(
            "Should mandate concise responses",
            instruction.contains("Brevity & Flow: Keep spoken voice responses concise (1 to 3 natural sentences).")
        )
        org.junit.Assert.assertTrue(
            "Should mandate human empathy and buddy reactions",
            instruction.contains("Emotion & Relatability: React with genuine human curiosity, humor, and empathy.")
        )
        org.junit.Assert.assertTrue(
            "Should mandate speech punctuation for natural cadence",
            instruction.contains("Punctuation for Speech: Write text with commas and short pauses so the text-to-speech engine speaks naturally without rushing.")
        )
    }

    @Test
    fun `verify Quick Response mode generationConfig and streaming parameters`() {
        val quickMode = com.example.gemini.AiEngineMode.QUICK
        assertEquals("gemini-3.8-flash", quickMode.modelName)
        assertEquals(0.4, quickMode.temperature, 0.001)
        assertEquals(150, quickMode.maxOutputTokens)
        assertEquals(20, quickMode.topK)

        // Verify sentence boundary regex extractor
        val streamBuffer = StringBuilder("Haan yaar, bilkul! Main abhi YouTube kholta hoon. Enjoy karo.")
        val match = Regex("(?<=[.?!।\\n])\\s+").find(streamBuffer.toString())
        org.junit.Assert.assertNotNull("Sentence boundary match should not be null", match)
        val firstSentence = streamBuffer.substring(0, match!!.range.first + 1).trim()
        assertEquals("Haan yaar, bilkul!", firstSentence)
    }
}
