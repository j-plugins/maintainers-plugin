package com.github.xepozz.maintainers.model

import com.github.xepozz.maintainers.providers.composer.ComposerPackageManager
import com.github.xepozz.maintainers.providers.npm.NpmPackageManager

data class SearchFilter(
    val textQuery: String = "",
    val fundingOnly: Boolean = false,
    val packageManagers: Set<PackageManager> = emptySet()
) {
    companion object {
        private val PM_PATTERN = Regex("""pm:(\w+)""")
        private const val FUNDING_FILTER = "is:funding"

        fun parse(text: String): SearchFilter {
            val pmNames = PM_PATTERN.findAll(text).map { it.groupValues[1] }.toSet()
            val pms = pmNames.mapNotNull { name ->
                when (name) {
                    NpmPackageManager.name -> NpmPackageManager
                    ComposerPackageManager.name -> ComposerPackageManager
                    else -> null
                }
            }.toSet()
            val fundingOnly = text.contains(FUNDING_FILTER)
            
            var cleanText = text.replace(FUNDING_FILTER, "")
            pmNames.forEach { cleanText = cleanText.replace("pm:$it", "") }
            
            return SearchFilter(
                textQuery = cleanText.trim().lowercase(),
                fundingOnly = fundingOnly,
                packageManagers = pms
            )
        }
    }
    
    fun toText(): String {
        val parts = mutableListOf<String>()
        if (fundingOnly) parts.add("is:funding")
        packageManagers.forEach { parts.add("pm:${it.name}") }
        if (textQuery.isNotEmpty()) parts.add(textQuery)
        return parts.joinToString(" ")
    }
}
