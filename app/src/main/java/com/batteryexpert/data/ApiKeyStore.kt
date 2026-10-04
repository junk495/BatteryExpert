package com.batteryexpert.data

import android.content.Context
import com.batteryexpert.BuildConfig

class ApiKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("battery_expert_prefs", Context.MODE_PRIVATE)

    enum class Provider { GEMINI, DEEPSEEK }

    fun getProvider(): Provider =
        if (prefs.getString(KEY_PROVIDER, "gemini") == "deepseek") Provider.DEEPSEEK else Provider.GEMINI

    fun setProvider(provider: Provider) {
        prefs.edit().putString(KEY_PROVIDER, provider.name.lowercase()).apply()
    }

    fun getApiKey(provider: Provider): String {
        val keyName = if (provider == Provider.GEMINI) KEY_GEMINI else KEY_DEEPSEEK
        val stored = prefs.getString(keyName, null)
        return stored?.takeIf { it.isNotBlank() }
            ?: if (provider == Provider.GEMINI) BuildConfig.GEMINI_API_KEY else ""
    }

    fun setApiKey(provider: Provider, key: String) {
        val keyName = if (provider == Provider.GEMINI) KEY_GEMINI else KEY_DEEPSEEK
        prefs.edit().putString(keyName, key.trim()).apply()
    }

    private companion object {
        const val KEY_PROVIDER = "ai_provider"
        const val KEY_GEMINI = "gemini_api_key"
        const val KEY_DEEPSEEK = "deepseek_api_key"
    }
}
