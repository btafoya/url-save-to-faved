package com.tafoyaventures.urlsavetofaved

import android.content.Context

enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]

    fun glyph(): String = when (this) {
        LIGHT -> "☀"
        DARK -> "🌙"
        SYSTEM -> "🌓"
    }
}

object ThemePreferenceStore {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_MODE = "mode"

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var mode: ThemeMode
        get() = ThemeMode.valueOf(prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name)!!)
        set(value) = prefs.edit().putString(KEY_MODE, value.name).apply()
}
