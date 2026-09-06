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

    private data class TagRow(val id: Int, val title: String, val color: String?, val parentId: Int?)

    // Faved's Repository::getTags() returns a PHP array keyed by tag id, which
    // json_encode()s as a JSON object (not array) since keys aren't sequential.
    // Tags are flat rows with a "parent" id (0 = root) — there's no server-side
    // nesting, so the tree is rebuilt here from each row's parent id.
    fun getTags(): List<FavedTag> {
        val serverUrl = requireServerUrl()
        val request = Request.Builder().url("$serverUrl/api/tags").build()

        val json = withReauth { client.newCall(request).execute() }.use { response ->
            if (!response.isSuccessful) error("Failed to load tags: HTTP ${response.code}")
            response.body?.string() ?: "{}"
        }

        val obj = JSONObject(json)
        val rows = obj.keys().asSequence().map { key ->
            val row = obj.getJSONObject(key)
            val parent = row.optInt("parent", 0)
            TagRow(
                id = row.getInt("id"),
                title = row.getString("title"),
                color = row.optNullableString("color"),
                parentId = if (parent == 0) null else parent
            )
        }.toList()

        return buildTagTree(rows)
    }

    private fun buildTagTree(rows: List<TagRow>): List<FavedTag> {
        val byParent = rows.groupBy { it.parentId }
        val result = mutableListOf<FavedTag>()

        fun addChildren(parentId: Int?, depth: Int) {
            byParent[parentId]?.sortedBy { it.title }?.forEach { row ->
                result += FavedTag(id = row.id, name = row.title, color = row.color, parentId = row.parentId, depth = depth)
                addChildren(row.id, depth + 1)
            }
        }

        addChildren(null, 0)
        return result
    }

    // Faved has no create-tag fields for color/parent_id: POST /api/tags takes only
    // a "/"-delimited title path (e.g. "Parent/Child") and auto-creates/reuses tags
    // along it, returning {data: {tag_id, title}}. Color can only be set afterward
    // via a separate update-color call, which this app doesn't offer on create.
    fun createTag(name: String, parentId: Int?, parentTitle: String?): FavedTag {
        val serverUrl = requireServerUrl()
        val title = if (parentTitle.isNullOrBlank()) name else "$parentTitle/$name"

        val body = JSONObject().put("title", title).toString().toRequestBody(JSON)
        val request = Request.Builder().url("$serverUrl/api/tags").post(body).build()

        val json = withReauth { client.newCall(request).execute() }.use { response ->
            if (!response.isSuccessful) error("Failed to create tag: HTTP ${response.code}")
            response.body?.string() ?: error("Empty response creating tag.")
        }

        val data = JSONObject(json).getJSONObject("data")
        return FavedTag(id = data.getInt("tag_id"), name = name, color = null, parentId = parentId, depth = 0)
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
