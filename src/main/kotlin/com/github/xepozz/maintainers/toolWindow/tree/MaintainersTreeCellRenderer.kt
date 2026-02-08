package com.github.xepozz.maintainers.toolWindow.tree

import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.toolWindow.details.AvatarLoader
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.tree.TreeUtil
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
        var userObject = TreeUtil.getUserObject(value) ?: return
        if (userObject is NodeDescriptor<*>) {
            userObject = userObject.element
        }

        when (userObject) {
            is GroupHeader -> {
                append(userObject.title)
                append(" (${userObject.count})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                icon = if (userObject.id == "dependencies") AllIcons.Nodes.Package else AllIcons.General.User
            }
            is PackageManagerGroup -> {
                append(userObject.packageManager.name)
                append(" (${userObject.count})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                icon = userObject.packageManager.icon
            }
            is String -> {
                append(userObject)
                icon = if (userObject.startsWith("Dependencies")) AllIcons.Nodes.Package else AllIcons.General.User
            }
            is Dependency -> {
                append(userObject.name)
                icon = userObject.source.icon
            }
            is Maintainer -> {
                append(userObject.name)
                if (userObject.packages.isNotEmpty()) {
                    append(" (${userObject.packages.size})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                
                val github = userObject.github
                val iconUrl = userObject.icon
                if (github != null) {
                    val cachedIcon = AvatarLoader.getIconIfLoaded(github, 16)
                    if (cachedIcon != null) {
                        icon = cachedIcon
                    } else {
                        icon = AllIcons.General.User
                        AvatarLoader.loadIcon(github, 16) {
                            tree.repaint()
                        }
                    }
                } else if (iconUrl != null) {
                    val cachedIcon = AvatarLoader.getIconByUrlIfLoaded(iconUrl, 16)
                    if (cachedIcon != null) {
                        icon = cachedIcon
                    } else {
                        icon = AllIcons.General.User
                        AvatarLoader.loadIconByUrl(iconUrl, 16) {
                            tree.repaint()
                        }
                    }
                } else {
                    icon = AllIcons.General.User
                }
            }
        }
    }
}
