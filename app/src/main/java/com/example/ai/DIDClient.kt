package com.example.ai

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DIDClient(context: Context) {
    private val secureStorage = SecureStorageManager(context)

    fun generateLipSyncVideo(imageUrl: String, text: String): String? {
        try {
            val apiKey = secureStorage.getDidApiKey().ifBlank { "YOUR_DID_API_KEY_BASE64" }
            if (apiKey == "YOUR_DID_API_KEY_BASE64" || apiKey.isBlank()) {
                return null
            }

            // 1. Create a Talk
            val createUrl = URL("https://api.d-id.com/talks")
            val createConn = createUrl.openConnection() as HttpURLConnection
            createConn.requestMethod = "POST"
            createConn.setRequestProperty("Authorization", "Basic $apiKey")
            createConn.setRequestProperty("Content-Type", "application/json")
            createConn.doOutput = true

            val scriptObj = JSONObject().apply {
                put("type", "text")
                put("input", text)
                put("provider", JSONObject().apply {
                    put("type", "microsoft")
                    put("voice_id", "en-US-JennyNeural") // Realistic female voice
                })
            }

            val payload = JSONObject().apply {
                put("source_url", imageUrl)
                put("script", scriptObj)
            }

            createConn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            if (createConn.responseCode !in 200..299) return null

            val createResponse = BufferedReader(InputStreamReader(createConn.inputStream)).use { it.readText() }
            val talkId = JSONObject(createResponse).getString("id")

            // 2. Poll for the Result Video
            var videoUrl: String? = null
            var retries = 0
            while (videoUrl == null && retries < 15) {
                Thread.sleep(1500) // Wait 1.5 seconds before checking

                val getUrl = URL("https://api.d-id.com/talks/$talkId")
                val getConn = getUrl.openConnection() as HttpURLConnection
                getConn.requestMethod = "GET"
                getConn.setRequestProperty("Authorization", "Basic $apiKey")

                if (getConn.responseCode in 200..299) {
                    val getResponse = BufferedReader(InputStreamReader(getConn.inputStream)).use { it.readText() }
                    val jsonResponse = JSONObject(getResponse)
                    val status = jsonResponse.optString("status")

                    if (status == "done") {
                        videoUrl = jsonResponse.optString("result_url")
                    } else if (status == "error") {
                        break
                    }
                }
                retries++
            }
            return videoUrl
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
