package com.tafoyaventures.urlsavetofaved

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataFetcherTest {

    private val baseUrl = "https://example.com/page"

    private fun parse(html: String) =
        MetadataFetcher.parseMetadata(Jsoup.parse(html, baseUrl), baseUrl)

    @Test
    fun `title prefers og over twitter over html title`() {
        val html = """
            <html><head>
              <title>HTML Title</title>
              <meta property="og:title" content="OG Title">
              <meta name="twitter:title" content="Twitter Title">
            </head></html>
        """
        assertEquals("OG Title", parse(html).title)
    }

    @Test
    fun `title falls back to twitter then html title`() {
        val twitterOnly = """
            <html><head>
              <title>HTML Title</title>
              <meta name="twitter:title" content="Twitter Title">
            </head></html>
        """
        assertEquals("Twitter Title", parse(twitterOnly).title)

        val htmlOnly = "<html><head><title>HTML Title</title></head></html>"
        assertEquals("HTML Title", parse(htmlOnly).title)
    }

    @Test
    fun `title is blank when nothing present`() {
        assertEquals("", parse("<html><head></head></html>").title)
    }

    @Test
    fun `description prefers og over twitter over meta description`() {
        val html = """
            <html><head>
              <meta name="description" content="Meta description">
              <meta name="twitter:description" content="Twitter description">
              <meta property="og:description" content="OG description">
            </head></html>
        """
        assertEquals("OG description", parse(html).description)
    }

    @Test
    fun `image resolves relative urls against final response url`() {
        val html = """
            <html><head>
              <meta property="og:image" content="/images/cover.png">
            </head></html>
        """
        assertEquals("https://example.com/images/cover.png", parse(html).imageUrl)
    }

    @Test
    fun `image falls back to twitter then link image_src`() {
        val linkOnly = """
            <html><head>
              <link rel="image_src" href="/thumb.png">
            </head></html>
        """
        assertEquals("https://example.com/thumb.png", parse(linkOnly).imageUrl)

        assertNull(parse("<html><head></head></html>").imageUrl)
    }

    @Test
    fun `canonical prefers og url over link canonical over final url`() {
        val html = """
            <html><head>
              <link rel="canonical" href="/canonical-page">
              <meta property="og:url" content="/og-page">
            </head></html>
        """
        assertEquals("https://example.com/og-page", parse(html).canonicalUrl)

        val linkOnly = """
            <html><head><link rel="canonical" href="/canonical-page"></head></html>
        """
        assertEquals("https://example.com/canonical-page", parse(linkOnly).canonicalUrl)

        assertEquals(baseUrl, parse("<html><head></head></html>").canonicalUrl)
    }

    @Test
    fun `blank meta content is ignored`() {
        val html = """
            <html><head>
              <meta property="og:title" content="   ">
              <title>Fallback Title</title>
            </head></html>
        """
        assertEquals("Fallback Title", parse(html).title)
    }
}
