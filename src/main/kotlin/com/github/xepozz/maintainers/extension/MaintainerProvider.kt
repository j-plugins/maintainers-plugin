package com.github.xepozz.maintainers.extension

import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.model.PackageInfo
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface MaintainerProvider {
    companion object {
        val EP_NAME = ExtensionPointName.create<MaintainerProvider>("com.github.xepozz.maintainers.maintainerProvider")

        fun getAggregatedMaintainers(project: Project): Map<Maintainer, List<Dependency>> {
            val providers = EP_NAME.extensionList
            val maintainerMap = mutableMapOf<String, Maintainer>()
            val maintainerDependencies = mutableMapOf<String, MutableSet<Dependency>>()

            providers.forEach { provider ->
                provider.getDependencies(project).forEach { dependency ->
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
                                fundingLinks = (existing.fundingLinks + maintainer.fundingLinks).distinctBy { it.url }
                            )
                        }
                        maintainerDependencies.getOrPut(key) { mutableSetOf() }.add(dependency)
                    }
                }
            }

            return maintainerMap.values.map { maintainer ->
                val dependencies = maintainerDependencies[maintainer.name] ?: emptySet()
                maintainer.copy(
                    packages = dependencies.map { PackageInfo(it.name, it.version) }
                )
            }.associateWith { maintainer -> maintainerDependencies[maintainer.name]?.toList() ?: emptyList() }
        }
    }

    /**
     * Returns a collection of dependencies with their maintainers found in the project.
     */
    fun getDependencies(project: Project): Collection<Dependency>
}
