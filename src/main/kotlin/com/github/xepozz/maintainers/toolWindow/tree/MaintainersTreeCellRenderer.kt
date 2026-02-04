package com.github.xepozz.maintainers.toolWindow.tree

import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JTree

class MaintainersTreeCellRenderer : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val node = value as? MaintainersTreeNode ?: return
        val userObject = node.userObject

        when (userObject) {
            is String -> {
                append(userObject)
                icon = if (userObject.startsWith("Dependencies")) AllIcons.Nodes.Package else AllIcons.General.User
            }
            is Dependency -> {
                append(userObject.name)
                icon = AllIcons.Nodes.Package
            }
            is Maintainer -> {
                append(userObject.name)
                if (userObject.packages.isNotEmpty()) {
                    append(" (${userObject.packages.size})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                icon = AllIcons.General.User
            }
        }
    }
}
