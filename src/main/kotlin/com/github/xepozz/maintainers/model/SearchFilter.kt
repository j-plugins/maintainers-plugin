package com.github.xepozz.maintainers.model

import com.github.xepozz.maintainers.extension.MaintainerProvider

data class SearchFilter(
    val textQuery: String = "",
    val fundingOnly: Boolean = false,
    val packageManagers: Set<PackageManager> = emptySet()
) {
    companion object {
        private const val FUNDING_FILTER = "is:funding"

        fun parse(text: String): SearchFilter {
            val allPms = MaintainerProvider.getAllPackageManagers()
            val matchedPms = mutableSetOf<PackageManager>()
            var cleanText = text

            for (pm in allPms.sortedByDescending { it.name.length }) {
                val tag = "pm:${pm.name}"
                if (cleanText.contains(tag)) {
                    matchedPms.add(pm)
                    cleanText = cleanText.replace(tag, "")
                }
            }

            val fundingOnly = cleanText.contains(FUNDING_FILTER)
            cleanText = cleanText.replace(FUNDING_FILTER, "")

            return SearchFilter(
                textQuery = cleanText.trim().lowercase(),
                fundingOnly = fundingOnly,
                packageManagers = matchedPms
            )
        }
    }

    fun toText(): String {
        return buildList {
            if (fundingOnly) {
                add("is:funding")
            }
            packageManagers.forEach {
                add("pm:${it.name}")
            }
            if (textQuery.isNotEmpty()) {
                add(textQuery)
            }
        }.joinToString(" ")
    }
}
