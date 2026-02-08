package com.github.xepozz.maintainers.toolWindow.tree

import com.github.xepozz.maintainers.MaintainersBundle
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.model.PackageManager
import com.github.xepozz.maintainers.model.SearchFilter
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project

class MaintainersTreeStructure(private val project: Project) : AbstractTreeStructure() {
    private var maintainerMap: Map<Maintainer, List<Dependency>> = emptyMap()
    private var allDependencies: List<Dependency> = emptyList()
    private var filterText: String = ""
    private val root = Any()
    var groupByPackageManager: Boolean = false
    var groupByPrefix: Boolean = false

    fun updateData(newMap: Map<Maintainer, List<Dependency>>, allDeps: List<Dependency>) {
        maintainerMap = newMap
        allDependencies = allDeps
    }

    fun setFilter(filter: SearchFilter) {
        currentFilter = filter
    }

    private var currentFilter: SearchFilter = SearchFilter()

    override fun getRootElement(): Any = root

    override fun getChildElements(element: Any): Array<Any> {
        return when (element) {
            root -> {
                val filteredMap = getFilteredMap()
                val filteredDependencies = getFilteredDependencies()
                arrayOf(
                    GroupHeader("maintainers", MaintainersBundle.message("tree.group.maintainers"), filteredMap.size),
                    GroupHeader("dependencies", MaintainersBundle.message("tree.group.dependencies"), filteredDependencies.size)
                )
            }
            is GroupHeader -> {
                if (element.id == "dependencies") {
                    val deps = getFilteredDependencies()
                        .distinctBy { it.name }
                        .sortedBy { it.name }

                    if (groupByPackageManager && groupByPrefix) {
                        deps.groupBy { it.source }
                            .map { (pm, dependencies) ->
                                PackageManagerGroup(pm, dependencies.size, true)
                            }
                            .sortedBy { it.packageManager.name }
                            .toTypedArray()
                    } else if (groupByPackageManager) {
                        deps.groupBy { it.source }
                            .map { (pm, dependencies) ->
                                PackageManagerGroup(pm, dependencies.size, false)
                            }
                            .sortedBy { it.packageManager.name }
                            .toTypedArray()
                    } else if (groupByPrefix) {
                        val grouped = deps.groupBy { it.groupPrefix }
                        val result = mutableListOf<Any>()
                        
                        // Items with prefix
                        grouped.filter { it.key != null }
                            .map { (prefix, dependencies) ->
                                DependencyGroup(prefix!!, dependencies.size)
                            }
                            .sortedBy { it.title }
                            .let { result.addAll(it) }
                            
                        // Items without prefix
                        grouped[null]?.let { result.addAll(it) }
                        
                        result.toTypedArray()
                    } else {
                        deps.toTypedArray()
                    }
                } else {
                    getFilteredMap().keys.sortedByDescending { it.packages.size }.toTypedArray()
                }
            }
            is PackageManagerGroup -> {
                val deps = getFilteredDependencies()
                    .filter { it.source == element.packageManager }
                    .distinctBy { it.name }
                    .sortedBy { it.name }
                
                if (element.nextGroupByPrefix) {
                    val grouped = deps.groupBy { it.groupPrefix }
                    val result = mutableListOf<Any>()
                    grouped.filter { it.key != null }
                        .map { (prefix, dependencies) ->
                            DependencyGroup(prefix!!, dependencies.size)
                        }
                        .sortedBy { it.title }
                        .let { result.addAll(it) }
                    grouped[null]?.let { result.addAll(it) }
                    result.toTypedArray()
                } else {
                    deps.toTypedArray()
                }
            }
            is DependencyGroup -> {
                getFilteredDependencies()
                    .filter { it.groupPrefix == element.title }
                    .distinctBy { it.name }
                    .sortedBy { it.name }
                    .toTypedArray()
            }
            is Dependency -> {
                val filter = currentFilter
                if (filter.textQuery.isEmpty() && !filter.fundingOnly && filter.packageManagers.isEmpty()) {
                    element.maintainers.toTypedArray()
                } else {
                    element.maintainers.filter { maintainer ->
                        matchesFilter(maintainer, listOf(element), filter)
                    }.toTypedArray()
                }
            }
            else -> emptyArray()
        }
    }

    private fun getFilteredDependencies(): List<Dependency> {
        return allDependencies.filter { matchesDependencyFilter(it, currentFilter) }
    }

    private fun getFilteredMap(): Map<Maintainer, List<Dependency>> {
        return maintainerMap.filter { (maintainer, dependencies) ->
            matchesFilter(maintainer, dependencies, currentFilter)
        }
    }

    private fun matchesFilter(maintainer: Maintainer, dependencies: List<Dependency>, filter: SearchFilter): Boolean {
        if (filter.fundingOnly && maintainer.fundingLinks.isEmpty()) return false
        if (filter.packageManagers.isNotEmpty() && !maintainer.packages.any { it.packageManager in filter.packageManagers }) return false

        if (filter.textQuery.isEmpty()) return true

        return maintainer.name.lowercase().contains(filter.textQuery) ||
                dependencies.any { it.name.lowercase().contains(filter.textQuery) }
    }

    private fun matchesDependencyFilter(dependency: Dependency, filter: SearchFilter): Boolean {
        if (filter.fundingOnly && !dependency.maintainers.any { it.fundingLinks.isNotEmpty() }) return false
        if (filter.packageManagers.isNotEmpty() && dependency.source !in filter.packageManagers) return false

        if (filter.textQuery.isEmpty()) return true

        return dependency.name.lowercase().contains(filter.textQuery) ||
                dependency.maintainers.any { it.name.lowercase().contains(filter.textQuery) }
    }

    override fun getParentElement(element: Any): Any? {
        return null 
    }

    override fun createDescriptor(element: Any, parentDescriptor: NodeDescriptor<*>?): NodeDescriptor<*> {
        return object : NodeDescriptor<Any>(project, parentDescriptor) {
            override fun update(): Boolean {
                val oldName = myName
                myName = when (element) {
                    is GroupHeader -> element.title
                    is PackageManagerGroup -> element.packageManager.name
                    is Dependency -> element.name
                    is Maintainer -> element.name
                    else -> element.toString()
                }
                return myName != oldName
            }
            override fun getElement(): Any = element
        }
    }

    override fun commit() {}
    override fun hasSomethingToCommit(): Boolean = false
}

data class PackageManagerGroup(val packageManager: PackageManager, val count: Int, val nextGroupByPrefix: Boolean = false)
data class DependencyGroup(val title: String, val count: Int)
