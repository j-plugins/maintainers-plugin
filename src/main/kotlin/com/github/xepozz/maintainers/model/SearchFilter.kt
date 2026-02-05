package com.github.xepozz.maintainers.model

data class SearchFilter(
    val textQuery: String = "",
    val fundingOnly: Boolean = false,
    val packageManagers: Set<String> = emptySet()
) {
    companion object {
        private val PM_PATTERN = Regex("""pm:(\w+)""")
        private const val FUNDING_FILTER = "is:funding"

        fun parse(text: String): SearchFilter {
            val pms = PM_PATTERN.findAll(text).map { it.groupValues[1] }.toSet()
            val fundingOnly = text.contains(FUNDING_FILTER)
            
            var cleanText = text.replace(FUNDING_FILTER, "")
            pms.forEach { cleanText = cleanText.replace("pm:$it", "") }
            
            return SearchFilter(
                textQuery = cleanText.trim().lowercase(),
                fundingOnly = fundingOnly,
                packageManagers = pms
            )
        }
    }
    
    fun toText(): String {
        val parts = mutableListOf<String>()
        if (textQuery.isNotEmpty()) parts.add(textQuery)
        if (fundingOnly) parts.add("is:funding")
        packageManagers.forEach { parts.add("pm:$it") }
        return parts.joinToString(" ")
    }
}
