package com.panther742.panther.core

import android.content.Context
import android.content.SharedPreferences

/** Persisted user configuration. Stored only on-device (plain SharedPreferences). */
data class PantherSettings(
    val provider: String = PROVIDER_OPENAI,   // openai | groq | gemini | custom
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = "",                 // only used for "custom" OpenAI-compatible endpoints
    val alwaysListen: Boolean = false,
    val userName: String = "Boss",
) {
    companion object {
        const val PROVIDER_OPENAI = "openai"
        const val PROVIDER_GROQ = "groq"
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_CUSTOM = "custom"
    }
}

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("panther_settings", Context.MODE_PRIVATE)

    fun load(): PantherSettings = PantherSettings(
        provider = prefs.getString(KEY_PROVIDER, PantherSettings.PROVIDER_OPENAI)
            ?: PantherSettings.PROVIDER_OPENAI,
        apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        model = prefs.getString(KEY_MODEL, "") ?: "",
        baseUrl = prefs.getString(KEY_BASE_URL, "") ?: "",
        alwaysListen = prefs.getBoolean(KEY_ALWAYS_LISTEN, false),
        userName = prefs.getString(KEY_USER_NAME, "Boss") ?: "Boss",
    )

    fun save(s: PantherSettings) {
        prefs.edit()
            .putString(KEY_PROVIDER, s.provider)
            .putString(KEY_API_KEY, s.apiKey)
            .putString(KEY_MODEL, s.model)
            .putString(KEY_BASE_URL, s.baseUrl)
            .putBoolean(KEY_ALWAYS_LISTEN, s.alwaysListen)
            .putString(KEY_USER_NAME, s.userName)
            .apply()
    }

    fun hasKey(): Boolean = !(prefs.getString(KEY_API_KEY, "") ?: "").isBlank()

    private companion object {
        const val KEY_PROVIDER = "provider"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
        const val KEY_BASE_URL = "base_url"
        const val KEY_ALWAYS_LISTEN = "always_listen"
        const val KEY_USER_NAME = "user_name"
    }
}
