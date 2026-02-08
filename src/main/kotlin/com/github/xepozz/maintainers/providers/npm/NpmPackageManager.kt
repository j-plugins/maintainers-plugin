package com.github.xepozz.maintainers.providers.npm

import com.github.xepozz.maintainers.MaintainersIcons
import com.github.xepozz.maintainers.model.PackageManager
import javax.swing.Icon

object NpmPackageManager : PackageManager {
    override val name: String = "NPM"
    override val icon: Icon = MaintainersIcons.NPM
}