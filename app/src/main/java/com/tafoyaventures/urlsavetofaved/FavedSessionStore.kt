package com.tafoyaventures.urlsavetofaved

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object FavedSessionStore {
    private const val PREFS_NAME = "faved_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_SESSION_COOKIE = "session_cookie"
    private const val KEY_CSRF_TOKEN = "csrf_token"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var password: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    var sessionCookie: String?
        get() = prefs.getString(KEY_SESSION_COOKIE, null)
        set(value) = prefs.edit().putString(KEY_SESSION_COOKIE, value).apply()

    var csrfToken: String?
        get() = prefs.getString(KEY_CSRF_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_CSRF_TOKEN, value).apply()

    fun isConfigured(): Boolean =
        !serverUrl.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()

    fun saveConfig(config: FavedConfig) {
        serverUrl = config.serverUrl
        username = config.username
        password = config.password
    }

    fun currentConfig(): FavedConfig? {
        val s = serverUrl
        val u = username
        val p = password
        return if (!s.isNullOrBlank() && !u.isNullOrBlank() && !p.isNullOrBlank()) FavedConfig(s, u, p) else null
    }
}
