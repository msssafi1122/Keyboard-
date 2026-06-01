package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GroqService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getAiResponse(apiKey: String, model: String, prompt: String): String {
        if (apiKey.isBlank()) {
            return "Please configure your Groq API Key in the Keyboard settings first!"
        }

        val url = "https://api.groq.com/openai/v1/chat/completions"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val jsonBody = JSONObject().apply {
            put("model", model)
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are Bangla AI Keyboard Assistant. Keep answers concisely tailored for a tiny keyboard screen. Answer in Bangla if possible, or matches the query language.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 512)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        return kotlinx.coroutines.Dispatchers.IO.let { dispatcher ->
            kotlinx.coroutines.withContext(dispatcher) {
                try {
                    client.newCall(request).execute().use { response ->
                        val respStr = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            Log.e("GroqService", "Error code: ${response.code} body: $respStr")
                            try {
                                val errorObj = JSONObject(respStr)
                                val errorDetail = errorObj.getJSONObject("error").getString("message")
                                return@withContext "API Error: $errorDetail"
                            } catch (parseEx: Exception) {
                                return@withContext "API Error: Code ${response.code}\n$respStr"
                            }
                        }
                        
                        val jsonResponse = JSONObject(respStr)
                        val choices = jsonResponse.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val message = choice.getJSONObject("message")
                            message.getString("content")
                        } else {
                            "Done. No textual response was received."
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GroqService", "Network Exception: ", e)
                    "Connection failed. Please ensure your key is valid and you are online.\nError: ${e.localizedMessage}"
                }
            }
        }
    }
}
