package com.github.xepozz.maintainers.toolWindow.tree

import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import javax.swing.tree.DefaultMutableTreeNode

class MaintainersTreeNode(userObject: Any) : DefaultMutableTreeNode(userObject) {
    val isMaintainer: Boolean get() = userObject is Maintainer
    val isPackage: Boolean get() = userObject is Dependency
    val isRootGroup: Boolean get() = userObject is String
}
