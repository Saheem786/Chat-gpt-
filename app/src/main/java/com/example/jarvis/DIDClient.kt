package com.example.jarvis

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DIDClient {
    // API KEY DAALEIN: Yahan apni base64 encoded D-ID API key paste karein
    private val didApiKey = "YOUR_DID_API_KEY_BASE64_HERE" 
    
    fun generateLipSyncVideo(imageUrl: String, text: String): String? {
        try {
            val createUrl = URL("https://api.d-id.com/talks")
            val createConn = createUrl.openConnection() as HttpURLConnection
            createConn.requestMethod = "POST"
            createConn.setRequestProperty("Authorization", "Basic $didApiKey")
            createConn.setRequestProperty("Content-Type", "application/json")
            createConn.doOutput = true

            val scriptObj = JSONObject().apply {
                put("type", "text")
                put("input", text)
                put("provider", JSONObject().apply {
                    put("type", "microsoft")
                    put("voice_id", "en-US-JennyNeural") 
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

            var videoUrl: String? = null
            var retries = 0
            while (videoUrl == null && retries < 15) {
                Thread.sleep(1500) 
                
                val getUrl = URL("https://api.d-id.com/talks/$talkId")
                val getConn = getUrl.openConnection() as HttpURLConnection
                getConn.requestMethod = "GET"
                getConn.setRequestProperty("Authorization", "Basic $didApiKey")

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
