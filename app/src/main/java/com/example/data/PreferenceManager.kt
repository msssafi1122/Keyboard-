package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME = "keyboard_theme"
        const val KEY_HEIGHT_PORTRAIT = "keyboard_height_portrait_new" // changed to avoid cache collision
        const val KEY_HEIGHT_LANDSCAPE = "keyboard_height_landscape_new"
        const val KEY_VIBRATION_ENABLED = "keyboard_vibration_enabled"
        const val KEY_VIBRATION_MS = "keyboard_vibration_ms"
        const val KEY_SOUND_ENABLED = "keyboard_sound_enabled"
        const val KEY_POPUP_ENABLED = "keyboard_popup_enabled"
        const val KEY_AUTO_CAPITALIZE = "keyboard_auto_capitalize"
        const val KEY_AUTO_SPACE = "keyboard_auto_space"
        const val KEY_AUTO_CORRECTION = "keyboard_auto_correction"
        const val KEY_SUGGESTION_STRIP = "keyboard_suggestion_strip"
        const val KEY_LAYOUT = "keyboard_layout"
        const val KEY_GROQ_API_KEY = "keyboard_groq_api_key_v2"
        const val KEY_AI_MODEL = "keyboard_ai_model"
    }

    var theme: String
        get() = prefs.getString(KEY_THEME, "Cosmic Dark") ?: "Cosmic Dark"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var heightPortrait: Float
        get() = prefs.getFloat(KEY_HEIGHT_PORTRAIT, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_HEIGHT_PORTRAIT, value).apply()

    var heightLandscape: Float
        get() = prefs.getFloat(KEY_HEIGHT_LANDSCAPE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_HEIGHT_LANDSCAPE, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var vibrationMs: Int
        get() = prefs.getInt(KEY_VIBRATION_MS, 40)
        set(value) = prefs.edit().putInt(KEY_VIBRATION_MS, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isPopupEnabled: Boolean
        get() = prefs.getBoolean(KEY_POPUP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_POPUP_ENABLED, value).apply()

    var isAutoCapitalizeEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CAPITALIZE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CAPITALIZE, value).apply()

    var isAutoSpaceEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPACE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPACE, value).apply()

    var isAutoCorrectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CORRECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CORRECTION, value).apply()

    var isSuggestionStripEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUGGESTION_STRIP, true)
        set(value) = prefs.edit().putBoolean(KEY_SUGGESTION_STRIP, value).apply()

    var activeLayout: String
        get() = prefs.getString(KEY_LAYOUT, "Phonetic") ?: "Phonetic"
        set(value) = prefs.edit().putString(KEY_LAYOUT, value).apply()

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ_API_KEY, value).apply()

    var aiModel: String
        get() = prefs.getString(KEY_AI_MODEL, "openai/gpt-oss-120b") ?: "openai/gpt-oss-120b"
        set(value) = prefs.edit().putString(KEY_AI_MODEL, value).apply()
}
