package com.github.xepozz.maintainers.providers.go

import com.github.xepozz.maintainers.MaintainersIcons
import com.github.xepozz.maintainers.model.PackageManager
import javax.swing.Icon

object GoPackageManager : PackageManager {
    override val name: String = "Go"
    override val icon: Icon = MaintainersIcons.GO
}