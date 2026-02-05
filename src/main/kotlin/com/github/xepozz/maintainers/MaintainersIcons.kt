package com.github.xepozz.maintainers

import com.intellij.openapi.util.IconLoader

// https://intellij-icons.jetbrains.design
// https://plugins.jetbrains.com/docs/intellij/icons.html#new-ui-tool-window-icons
// https://plugins.jetbrains.com/docs/intellij/icons-style.html
object MaintainersIcons {
    @JvmField
    val NPM = IconLoader.getIcon("/icons/npm/icon.svg", this::class.java)
    @JvmField
    val COMPOSER = IconLoader.getIcon("/icons/composer/icon.svg", this::class.java)
    @JvmField
    val GO = IconLoader.getIcon("/icons/go/icon.svg", this::class.java)
    @JvmField
    val MAINTAINERS = IconLoader.getIcon("/icons/maintainers/icon.svg", this::class.java)
}