package com.github.xepozz.maintainers.toolWindow.tree

import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project

class MaintainersTreeStructure(private val project: Project) : AbstractTreeStructure() {
    private var maintainerMap: Map<Maintainer, List<Dependency>> = emptyMap()
    private var filterText: String = ""
    private val root = Any()

    fun updateData(newMap: Map<Maintainer, List<Dependency>>) {
        maintainerMap = newMap
    }

    fun setFilter(filter: String) {
        filterText = filter.lowercase()
    }

    override fun getRootElement(): Any = root

    override fun getChildElements(element: Any): Array<Any> {
        return when (element) {
            root -> {
                val filteredMap = getFilteredMap()
                val dependenciesCount = filteredMap.values.flatten().distinctBy { it.name }.size
                arrayOf(
                    GroupHeader("Dependencies", dependenciesCount),
                    GroupHeader("Maintainers", filteredMap.size)
                )
            }
            is GroupHeader -> {
                val filteredMap = getFilteredMap()
                if (element.title == "Dependencies") {
                    filteredMap.values
                        .flatten()
                        .distinctBy { it.name }
                        .sortedBy { it.name }
                        .toTypedArray()
                } else {
                    filteredMap.keys.sortedByDescending { it.packages.size }.toTypedArray()
                }
            }
            is Dependency -> {
                val text = filterText
                if (text.isEmpty()) {
                    element.maintainers.toTypedArray()
                } else {
                    element.maintainers.filter { 
                        it.name.lowercase().contains(text) || 
                                it.packages.any { pkg -> pkg.name.lowercase().contains(text) }
                    }.toTypedArray()
                }
            }
            else -> emptyArray()
        }
    }

    private fun getFilteredMap(): Map<Maintainer, List<Dependency>> {
        val text = filterText
        if (text.isEmpty()) return maintainerMap
        
        return maintainerMap.filter { (maintainer, dependencies) ->
            maintainer.name.lowercase().contains(text) || 
                    dependencies.any { it.name.lowercase().contains(text) }
        }
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
