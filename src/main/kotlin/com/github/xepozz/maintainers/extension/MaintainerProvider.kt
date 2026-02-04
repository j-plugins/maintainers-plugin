package com.github.xepozz.maintainers.extension

import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface MaintainerProvider {
    companion object {
        val EP_NAME = ExtensionPointName.create<MaintainerProvider>("com.github.xepozz.maintainers.maintainerProvider")

        fun getAggregatedMaintainers(project: Project): Map<Maintainer, Int> {
            val providers = EP_NAME.extensionList
            val maintainerCounts = mutableMapOf<Maintainer, Int>()

            providers.forEach { provider ->
                provider.getDependencies(project).forEach { dependency ->
                    dependency.maintainers.forEach { maintainer ->
                        maintainerCounts[maintainer] = maintainerCounts.getOrDefault(maintainer, 0) + 1
                    }
                }
            }

            return maintainerCounts
        }
    }

    /**
     * Returns a collection of dependencies with their maintainers found in the project.
     */
    fun getDependencies(project: Project): Collection<Dependency>
}
