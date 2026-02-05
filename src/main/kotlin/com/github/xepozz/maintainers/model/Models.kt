package com.github.xepozz.maintainers.model

import javax.swing.Icon

data class FundingSource(
    val type: String,
    val url: String
)

data class PackageInfo(
    val name: String,
    val version: String,
    val packageManager: PackageManager,
    val role: String = "maintainer"
)

data class Maintainer(
    val name: String,
    val email: String? = null,
    val homepage: String? = null,
    val github: String? = null,
    val icon: String? = null,
    val fundingLinks: List<FundingSource> = emptyList(),
    val packages: List<PackageInfo> = emptyList()
)

data class Dependency(
    val name: String,
    val version: String,
    val source: PackageManager,
    val url: String? = null,
    val maintainers: List<Maintainer> = emptyList()
)

data class MaintainersStats(
    val maintainersCount: Int,
    val packagesCount: Int,
    val withFundingCount: Int,
    val topMaintainers: List<Maintainer>
)

data class AggregatedData(
    val maintainerMap: Map<Maintainer, List<Dependency>>,
    val allDependencies: List<Dependency>
)

interface PackageManager {
    val name: String
    val icon: Icon
}