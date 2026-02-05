package com.github.xepozz.maintainers.providers.composer

import com.github.xepozz.maintainers.MaintainersIcons
import com.github.xepozz.maintainers.model.PackageManager
import com.intellij.icons.AllIcons
import javax.swing.Icon

object ComposerPackageManager : PackageManager {
    override val name: String = "composer"
    override val icon: Icon = MaintainersIcons.COMPOSER
}