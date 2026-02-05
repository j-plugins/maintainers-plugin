package com.github.xepozz.maintainers.providers.npm

import com.github.xepozz.maintainers.MaintainersIcons
import com.github.xepozz.maintainers.model.PackageManager
import com.intellij.icons.AllIcons
import javax.swing.Icon

object NpmPackageManager : PackageManager {
    override val name: String = "npm"
    override val icon: Icon = MaintainersIcons.NPM
}