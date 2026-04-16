package com.github.xepozz.maintainers.testutil

import com.github.xepozz.maintainers.model.PackageManager
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

/**
 * Stub icon used by test-only [PackageManager] implementations. Keeps tests independent of
 * the IntelliJ icon loader, which requires a running platform.
 */
private val TEST_ICON: Icon = object : Icon {
    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {}
    override fun getIconWidth(): Int = 16
    override fun getIconHeight(): Int = 16
}

/** Generic test package manager used when the specific source doesn't matter. */
object TestPackageManager : PackageManager {
    override val name: String = "test"
    override val icon: Icon = TEST_ICON
}

/** Second distinct package manager for cross-provider aggregation tests. */
object OtherTestPackageManager : PackageManager {
    override val name: String = "other"
    override val icon: Icon = TEST_ICON
}
