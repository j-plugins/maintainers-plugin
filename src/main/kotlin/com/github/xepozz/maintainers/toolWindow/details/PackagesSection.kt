package com.github.xepozz.maintainers.toolWindow.details

import com.github.xepozz.maintainers.model.PackageInfo
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JPanel

class PackagesSection : JPanel() {
    private var isExpanded = false
    private var currentPackages: List<PackageInfo> = emptyList()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.emptyTop(10)
    }

    fun update(packages: List<PackageInfo>) {
        currentPackages = packages
        isExpanded = false
        render()
    }

    private fun render() {
        removeAll()
        if (currentPackages.isEmpty()) {
            isVisible = false
            return
        }
        isVisible = true

        val header = JBLabel("PACKAGES").apply {
            font = JBFont.label().asBold()
            foreground = JBColor.namedColor("Label.disabledForeground", JBColor.GRAY)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        add(header)

        val displayPackages = if (isExpanded || currentPackages.size <= 5) {
            currentPackages
        } else {
            currentPackages.take(5)
        }

        displayPackages.forEach { pkg ->
            val label = JBLabel("• ${pkg.name} (${pkg.version})").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                border = JBUI.Borders.empty(2, 0)
            }
            add(label)
        }

        if (!isExpanded && currentPackages.size > 5) {
            val moreLabel = JBLabel("+ ${currentPackages.size - 5} more...").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                foreground = JBColor.namedColor("link.foreground", JBColor(0x589df6, 0x589df6))
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        isExpanded = true
                        render()
                        revalidate()
                        repaint()
                    }
                })
            }
            add(moreLabel)
        }

        revalidate()
        repaint()
    }
}
