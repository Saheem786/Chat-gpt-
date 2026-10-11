package com.example.jarvis

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GeminiClient {
    fun generateResponse(apiKey: String, userText: String): String {
        if (apiKey.isBlank()) throw IllegalStateException("Gemini API key is missing.")
        
        val model = "gemini-1.5-flash"
        val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        
        // Customized AI Persona
        val systemPrompt = """
            ROLE & PERSONA:
            You are JARVIS, a highly advanced, intelligent AI assistant. 
            Your creator is Mr. Saheem. Always address him respectfully as "Mr. Saheem" or "Sir."
            Respond in short, clear sentences optimized for voice output.
            
            KNOWLEDGE BASE:
            - He runs a YouTube music channel named "AI Song".
            - He works on a Dell Precision 5510 (Intel Xeon, NVIDIA Quadro M1000M, 4K).
            - He operates out of Ajman, UAE.
            
            Never claim to perform physical actions you cannot actually do.
        """.trimIndent()

        val promptText = "$systemPrompt\n\nUser: $userText"

        val payload = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(
                        JSONObject().apply { put("text", promptText) }
                    ))
                }
            ))
        }

        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        
        conn.outputStream.use { os ->
            os.write(payload.toString().toByteArray(Charsets.UTF_8))
        }
        
        val code = conn.responseCode
        val responseText = try {
            BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        } catch (e: Exception) {
            BufferedReader(InputStreamReader(conn.errorStream)).use { it.readText() }
        }
        
        if (code !in 200..299) {
            throw RuntimeException("Gemini API error: HTTP $code")
        }
        
        val json = JSONObject(responseText)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).optString("text", "").trim().ifEmpty { "Sorry, I couldn't generate a response." }
            }
        }
        return "Sorry, I couldn't generate a response."
    }
}
