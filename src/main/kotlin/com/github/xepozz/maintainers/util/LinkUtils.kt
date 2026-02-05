package com.github.xepozz.maintainers.util

/**
 * Deduplicates links by removing prefixes like "https://", "http://", "www." 
 * and keeping only unique base URLs.
 * 
 * Requirements:
 * 1. Ignore "https://", "http://", "www." prefixes in any combination.
 * 2. Return unique links without these prefixes.
 * 3. Preserve the original order of the first occurrence.
 * 4. Case-insensitive comparison.
 */
fun deduplicateLinks(links: List<String>): List<String> {
    val prefixRegex = Regex("^(https?://)?(www\\.)?", RegexOption.IGNORE_CASE)
    val seen = mutableSetOf<String>()
    val result = mutableListOf<String>()

    for (link in links) {
        var currentLink = link
        var cleanLink: String
        while (true) {
            cleanLink = currentLink.replace(prefixRegex, "")
            if (cleanLink == currentLink) break
            currentLink = cleanLink
        }
        val lowerCleanLink = cleanLink.lowercase()
        
        if (seen.add(lowerCleanLink)) {
            result.add(cleanLink)
        }
    }

    return result
}
