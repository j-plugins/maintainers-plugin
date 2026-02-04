package com.github.xepozz.maintainers.model

data class FundingSource(
    val type: String,
    val url: String
)

data class Maintainer(
    val name: String,
    val email: String? = null,
    val url: String? = null,
    val avatarUrl: String? = null,
    val funding: List<FundingSource> = emptyList()
)

data class Dependency(
    val name: String,
    val version: String,
    val source: String,
    val url: String? = null,
    val maintainers: List<Maintainer> = emptyList()
)
