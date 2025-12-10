package com.alessandrolattao.lanotifica.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `normalizeUrl adds https when no protocol`() {
        assertEquals("https://example.com/", UrlUtils.normalizeUrl("example.com"))
    }

    @Test
    fun `normalizeUrl preserves https protocol`() {
        assertEquals("https://example.com/", UrlUtils.normalizeUrl("https://example.com"))
    }

    @Test
    fun `normalizeUrl preserves http protocol`() {
        assertEquals("http://example.com/", UrlUtils.normalizeUrl("http://example.com"))
    }

    @Test
    fun `normalizeUrl adds trailing slash`() {
        assertEquals("https://example.com/", UrlUtils.normalizeUrl("https://example.com"))
    }

    @Test
    fun `normalizeUrl preserves existing trailing slash`() {
        assertEquals("https://example.com/", UrlUtils.normalizeUrl("https://example.com/"))
    }

    @Test
    fun `normalizeUrl handles mDNS hostname`() {
        assertEquals(
            "https://lanotifica.local:19420/",
            UrlUtils.normalizeUrl("lanotifica.local:19420"),
        )
    }

    @Test
    fun `normalizeUrl trims whitespace`() {
        assertEquals("https://example.com/", UrlUtils.normalizeUrl("  example.com  "))
    }

    @Test
    fun `normalizeUrl handles full URL with port and path`() {
        assertEquals(
            "https://localhost:8080/api/",
            UrlUtils.normalizeUrl("https://localhost:8080/api"),
        )
    }

    @Test
    fun `normalizeUrl handles IP address`() {
        assertEquals("https://192.168.1.100:19420/", UrlUtils.normalizeUrl("192.168.1.100:19420"))
    }
}
