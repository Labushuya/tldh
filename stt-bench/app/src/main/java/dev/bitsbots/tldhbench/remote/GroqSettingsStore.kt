package dev.bitsbots.tldhbench.remote

import android.content.Context

class GroqSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("groq_remote_settings", Context.MODE_PRIVATE)

    fun apiKey(): String = prefs.getString(KEY_API_KEY, "").orEmpty()

    fun hasApiKey(): Boolean = apiKey().isNotBlank()

    fun saveApiKey(value: String) {
        prefs.edit().putString(KEY_API_KEY, value.trim()).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    fun prompt(): String = prefs.getString(KEY_PROMPT, DEFAULT_PROMPT).orEmpty().ifBlank { DEFAULT_PROMPT }

    fun savePrompt(value: String) {
        prefs.edit().putString(KEY_PROMPT, value.trim()).apply()
    }

    companion object {
        private const val KEY_API_KEY = "groq_api_key"
        private const val KEY_PROMPT = "groq_prompt"

        const val DEFAULT_PROMPT: String =
            "Deutschsprachige WhatsApp-Sprachnachricht. Transkribiere auf Deutsch, behalte Umgangssprache, Namen, Negationen, Zahlen und Uhrzeiten möglichst exakt bei."
    }
}
