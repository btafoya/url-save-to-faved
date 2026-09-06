package com.tafoyaventures.urlsavetofaved

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object MetadataFetcher {
    private const val MAX_BODY_BYTES = 2L * 1024 * 1024

    private fun isBlockedHost(host: String): Boolean = try {
        InetAddress.getAllByName(host).any {
            it.isLoopbackAddress || it.isSiteLocalAddress || it.isLinkLocalAddress || it.isAnyLocalAddress
        }
    } catch (_: Exception) {
        true
    }

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain ->
            val host = chain.request().url.host
            if (isBlockedHost(host)) throw IOException("Blocked host: $host")
            chain.proceed(chain.request())
        }
        .build()

    fun fetch(url: String): PageMetadata {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36"
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")

            val body = response.body ?: error("Empty response")
            val finalUrl = response.request.url.toString()
            val bytes = body.source().let { source ->
                source.request(MAX_BODY_BYTES)
                source.buffer.readByteArray(minOf(source.buffer.size, MAX_BODY_BYTES))
            }
            val document = Jsoup.parse(bytes.inputStream(), null, finalUrl)
            return parseMetadata(document, finalUrl)
        }
    }

    internal fun parseMetadata(document: Document, finalUrl: String): PageMetadata {
        fun meta(property: String): String? =
            document.selectFirst("meta[property=$property]")?.attr("content")
                ?.takeIf { it.isNotBlank() }

        fun nameMeta(name: String): String? =
            document.selectFirst("meta[name=$name]")?.attr("content")
                ?.takeIf { it.isNotBlank() }

        fun metaAbs(property: String): String? =
            document.selectFirst("meta[property=$property]")
                ?.takeIf { it.attr("content").isNotBlank() }
                ?.absUrl("content")
                ?.takeIf { it.isNotBlank() }

        fun nameMetaAbs(name: String): String? =
            document.selectFirst("meta[name=$name]")
                ?.takeIf { it.attr("content").isNotBlank() }
                ?.absUrl("content")
                ?.takeIf { it.isNotBlank() }

        fun linkAbs(rel: String): String? =
            document.selectFirst("link[rel=$rel]")
                ?.takeIf { it.attr("href").isNotBlank() }
                ?.absUrl("href")
                ?.takeIf { it.isNotBlank() }

        val title = meta("og:title")
            ?: nameMeta("twitter:title")
            ?: document.title().takeIf { it.isNotBlank() }
            ?: ""

        val description = meta("og:description")
            ?: nameMeta("twitter:description")
            ?: nameMeta("description")
            ?: ""

        val image = metaAbs("og:image")
            ?: nameMetaAbs("twitter:image")
            ?: linkAbs("image_src")

        val canonical = metaAbs("og:url")
            ?: linkAbs("canonical")
            ?: finalUrl

        return PageMetadata(
            title = title,
            description = description,
            imageUrl = image,
            canonicalUrl = canonical
        )
    }
}
