package com.github.xepozz.maintainers.providers.ide

import com.github.xepozz.maintainers.MaintainersIcons
import com.github.xepozz.maintainers.model.PackageManager
import javax.swing.Icon

object IdePackageManager : PackageManager {
    override val name: String = "IDE Plugins"
    override val icon: Icon = MaintainersIcons.JETBRAINS
}
