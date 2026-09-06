package com.tafoyaventures.urlsavetofaved

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object FavedApiClient {
    private val JSON = "application/json".toMediaType()

    // ponytail: hand-rolled cookie/CSRF handling instead of a persistent CookieJar
    // library — Faved only ever sets two cookies, not worth a dependency.
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()

            val cookies = listOfNotNull(
                FavedSessionStore.sessionCookie?.let { "faved-session=$it" },
                FavedSessionStore.csrfToken?.let { "CSRF-TOKEN=$it" }
            ).joinToString("; ")
            if (cookies.isNotBlank()) requestBuilder.header("Cookie", cookies)

            if (chain.request().method != "GET") {
                FavedSessionStore.csrfToken?.let { requestBuilder.header("X-CSRF-Token", it) }
            }

            val response = chain.proceed(requestBuilder.build())

            response.headers("Set-Cookie").forEach { setCookie ->
                val (name, value) = setCookie.substringBefore(';').split("=", limit = 2)
                    .let { it[0].trim() to it.getOrElse(1) { "" } }
                when (name) {
                    "faved-session" -> FavedSessionStore.sessionCookie = value
                    "CSRF-TOKEN" -> FavedSessionStore.csrfToken = value
                }
            }

            response
        }
        .build()

    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun normalizeServerUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
        else "https://$trimmed"
    }

    private fun requireServerUrl(): String =
        normalizeServerUrl(FavedSessionStore.serverUrl ?: error("Faved is not configured."))

    private fun ensureCsrfToken(serverUrl: String) {
        if (FavedSessionStore.csrfToken != null) return
        val request = Request.Builder().url("$serverUrl/api/app-info").build()
        client.newCall(request).execute().close()
    }

    fun login(config: FavedConfig) {
        val serverUrl = normalizeServerUrl(config.serverUrl)
        ensureCsrfToken(serverUrl)

        val body = JSONObject()
            .put("username", config.username)
            .put("password", config.password)
            .toString()
            .toRequestBody(JSON)

        val request = Request.Builder().url("$serverUrl/api/auth/login").post(body).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Sign in failed: HTTP ${response.code}")
        }

        FavedSessionStore.saveConfig(config.copy(serverUrl = serverUrl))
    }

    // ponytail: one reauth retry on 401, matches the 7-day Faved session TTL —
    // add exponential backoff if flaky networks make one retry insufficient.
    private fun withReauth(call: () -> Response): Response {
        val response = call()
        if (response.code != 401) return response
        response.close()

        login(FavedSessionStore.currentConfig() ?: error("Faved is not configured."))
        return call()
    }

    fun getTags(): List<FavedTag> {
        val serverUrl = requireServerUrl()
        val request = Request.Builder().url("$serverUrl/api/tags").build()

        val json = withReauth { client.newCall(request).execute() }.use { response ->
            if (!response.isSuccessful) error("Failed to load tags: HTTP ${response.code}")
            response.body?.string() ?: "[]"
        }

        return flattenTags(JSONArray(json), parentId = null, depth = 0)
    }

    private fun flattenTags(array: JSONArray, parentId: Int?, depth: Int): List<FavedTag> {
        val result = mutableListOf<FavedTag>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.getInt("id")
            result += FavedTag(
                id = id,
                name = obj.getString("name"),
                color = obj.optNullableString("color"),
                parentId = parentId,
                depth = depth
            )
            obj.optJSONArray("children")?.let { result += flattenTags(it, parentId = id, depth = depth + 1) }
        }
        return result
    }

    fun createTag(name: String, color: String?, parentId: Int?): FavedTag {
        val serverUrl = requireServerUrl()

        val body = JSONObject()
            .put("name", name)
            .put("color", color ?: JSONObject.NULL)
            .put("parent_id", parentId ?: JSONObject.NULL)
            .toString()
            .toRequestBody(JSON)

        val request = Request.Builder().url("$serverUrl/api/tags").post(body).build()

        val json = withReauth { client.newCall(request).execute() }.use { response ->
            if (!response.isSuccessful) error("Failed to create tag: HTTP ${response.code}")
            response.body?.string() ?: error("Empty response creating tag.")
        }

        val data = JSONObject(json).let { it.optJSONObject("data") ?: it }
        return FavedTag(
            id = data.getInt("id"),
            name = data.optString("name", name),
            color = data.optNullableString("color") ?: color,
            parentId = parentId,
            depth = 0
        )
    }

    fun createItem(title: String, url: String, description: String, image: String?, tagIds: List<Int>) {
        val serverUrl = requireServerUrl()

        val body = JSONObject()
            .put("title", title)
            .put("url", url)
            .put("description", description)
            .put("comments", "")
            .put("image", image ?: "")
            .put("tags", JSONArray(tagIds))
            .toString()
            .toRequestBody(JSON)

        val request = Request.Builder().url("$serverUrl/api/items").post(body).build()

        withReauth { client.newCall(request).execute() }.use { response ->
            if (!response.isSuccessful) error("Save failed: HTTP ${response.code}")
        }
    }
}
