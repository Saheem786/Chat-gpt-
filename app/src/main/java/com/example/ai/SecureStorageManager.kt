package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorageManager(context: Context) {
    companion object {
        private const val TAG = "JarvisSecureStorage"
        private const val PREF_NAME = "jarvis_secure_ai_prefs"
        private const val KEY_NINEROUTER_API_KEY = "ninerouter_api_key"
        private const val KEY_AI_PROVIDER = "ai_provider" // "GEMINI" or "NINEROUTER"
        private const val KEY_NINEROUTER_BASE_URL = "ninerouter_base_url"
        private const val KEY_NINEROUTER_MODEL = "ninerouter_model"
        private const val KEY_DID_API_KEY = "did_api_key"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create EncryptedSharedPreferences, falling back to MODE_PRIVATE", e)
            context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    fun getAiProvider(): String {
        return sharedPreferences.getString(KEY_AI_PROVIDER, "GEMINI") ?: "GEMINI"
    }

    fun setAiProvider(provider: String) {
        sharedPreferences.edit().putString(KEY_AI_PROVIDER, provider).apply()
    }

    fun getNineRouterApiKey(): String {
        return sharedPreferences.getString(KEY_NINEROUTER_API_KEY, "") ?: ""
    }

    fun setNineRouterApiKey(key: String) {
        sharedPreferences.edit().putString(KEY_NINEROUTER_API_KEY, key.trim()).apply()
    }

    fun getNineRouterBaseUrl(): String {
        return sharedPreferences.getString(KEY_NINEROUTER_BASE_URL, "http://localhost:20128") ?: "http://localhost:20128"
    }

    fun setNineRouterBaseUrl(url: String) {
        sharedPreferences.edit().putString(KEY_NINEROUTER_BASE_URL, url.trim()).apply()
    }

    fun getNineRouterModel(): String {
        return sharedPreferences.getString(KEY_NINEROUTER_MODEL, "") ?: ""
    }

    fun setNineRouterModel(model: String) {
        sharedPreferences.edit().putString(KEY_NINEROUTER_MODEL, model.trim()).apply()
    }

    fun getDidApiKey(): String {
        return sharedPreferences.getString(KEY_DID_API_KEY, "") ?: ""
    }

    fun setDidApiKey(key: String) {
        sharedPreferences.edit().putString(KEY_DID_API_KEY, key.trim()).apply()
    }
}
