package com.github.xepozz.maintainers.model

import com.github.xepozz.maintainers.extension.MaintainerProvider

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
            val pms = MaintainerProvider.getAllPackageManagers()
                .filter { pmNames.contains(it.name) }
                .toSet()
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
