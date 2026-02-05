package com.github.xepozz.maintainers.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkUtilsTest {

    @Test
    fun `test deduplicateLinks with various prefixes`() {
        val input = listOf(
            "https://github.com/roadrunner-server",
            "github.com/roadrunner-server",
            "http://www.example.com",
            "https://example.com",
            "HTTP://WWW.EXAMPLE.COM"
        )
        val expected = listOf(
            "github.com/roadrunner-server",
            "example.com"
        )
        assertEquals(expected, deduplicateLinks(input))
    }

    @Test
    fun `test deduplicateLinks preserves order`() {
        val input = listOf(
            "example.com",
            "https://github.com",
            "http://example.com"
        )
        val expected = listOf(
            "example.com",
            "github.com"
        )
        assertEquals(expected, deduplicateLinks(input))
    }

    @Test
    fun `test deduplicateLinks with combined prefixes`() {
        val input = listOf(
            "https://www.google.com",
            "http://google.com",
            "www.google.com",
            "google.com"
        )
        val expected = listOf(
            "google.com"
        )
        assertEquals(expected, deduplicateLinks(input))
    }

    @Test
    fun `test deduplicateLinks case insensitivity`() {
        val input = listOf(
            "GitHub.com/Xepozz",
            "https://github.com/xepozz"
        )
        // Should keep the casing of the first occurrence if we follow the current implementation
        // Actually, the current implementation does:
        // result.add(cleanLink) 
        // cleanLink is derived from currentLink which is derived from link.
        // So it keeps the casing of the first occurrence.
        val expected = listOf(
            "GitHub.com/Xepozz"
        )
        assertEquals(expected, deduplicateLinks(input))
    }

    @Test
    fun `test deduplicateLinks with empty list`() {
        val input = emptyList<String>()
        val expected = emptyList<String>()
        assertEquals(expected, deduplicateLinks(input))
    }

    @Test
    fun `test deduplicateLinks without prefixes`() {
        val input = listOf("a.com", "b.com", "a.com")
        val expected = listOf("a.com", "b.com")
        assertEquals(expected, deduplicateLinks(input))
    }
}
