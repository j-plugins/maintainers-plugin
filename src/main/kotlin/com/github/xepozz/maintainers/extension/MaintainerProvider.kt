package com.github.xepozz.maintainers.extension

import com.github.xepozz.maintainers.model.AggregatedData
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.model.PackageInfo
import com.github.xepozz.maintainers.model.PackageManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface MaintainerProvider {
    companion object {
        val EP_NAME = ExtensionPointName.create<MaintainerProvider>("com.github.xepozz.maintainers.maintainerProvider")

        fun getAllPackageManagers(): Set<PackageManager> = EP_NAME.extensionList
            .map { it.packageManager }
            .toSet()

        fun getAggregatedData(project: Project): AggregatedData {
            val providers = EP_NAME.extensionList
            val allDependencies = providers.flatMap { it.getDependencies(project) }
            return aggregate(allDependencies)
        }

        fun aggregate(allDependencies: Collection<Dependency>): AggregatedData {
            val maintainerMap = mutableMapOf<String, Maintainer>()
            val maintainerDependencies = mutableMapOf<String, MutableSet<Dependency>>()

            allDependencies.forEach { dependency ->
                dependency.maintainers.forEach { maintainer ->
                    val key = maintainer.name
                    val existing = maintainerMap[key]
                    if (existing == null) {
                        maintainerMap[key] = maintainer
                    } else {
                        maintainerMap[key] = existing.copy(
                            email = existing.email ?: maintainer.email,
                            homepage = existing.homepage ?: maintainer.homepage,
                            github = existing.github ?: maintainer.github,
                            icon = existing.icon ?: maintainer.icon,
                            fundingLinks = (existing.fundingLinks + maintainer.fundingLinks).distinctBy { it.url }
                        )
                    }
                    maintainerDependencies.getOrPut(key) { mutableSetOf() }.add(dependency)
                }
            }

            val finalMaintainerMap = maintainerMap.values.map { maintainer ->
                val dependencies = maintainerDependencies[maintainer.name] ?: emptySet()
                maintainer.copy(
                    packages = dependencies.map { PackageInfo(it.name, it.version, it.source) }
                )
            }.associateWith { maintainer -> maintainerDependencies[maintainer.name]?.toList() ?: emptyList() }

            return AggregatedData(finalMaintainerMap, allDependencies.toList())
        }
    }

    /**
     * Returns a collection of dependencies with their maintainers found in the project.
     */
    fun getDependencies(project: Project): Collection<Dependency>

    /**
     * The package manager associated with this provider.
     */
    val packageManager: com.github.xepozz.maintainers.model.PackageManager
}
