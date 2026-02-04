package com.github.xepozz.maintainers.toolWindow.tree

import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import javax.swing.tree.DefaultTreeModel

class MaintainersTreeModel : DefaultTreeModel(MaintainersTreeNode("Root")) {
    fun updateData(maintainerMap: Map<Maintainer, List<Dependency>>) {
        val root = root as MaintainersTreeNode
        root.removeAllChildren()

        val dependenciesNode = MaintainersTreeNode("Dependencies (${maintainerMap.values.flatten().distinctBy { it.name }.size})")
        val byMaintainerNode = MaintainersTreeNode("By Maintainer")

        // Dependencies branch
        maintainerMap.values.flatten().distinctBy { it.name }.sortedBy { it.name }.forEach { dependency ->
            val depNode = MaintainersTreeNode(dependency)
            dependency.maintainers.forEach { maintainer ->
                depNode.add(MaintainersTreeNode(maintainer))
            }
            dependenciesNode.add(depNode)
        }

        // By Maintainer branch
        maintainerMap.keys.sortedByDescending { it.packages.size }.forEach { maintainer ->
            byMaintainerNode.add(MaintainersTreeNode(maintainer))
        }

        root.add(dependenciesNode)
        root.add(byMaintainerNode)

        reload()
    }
}
