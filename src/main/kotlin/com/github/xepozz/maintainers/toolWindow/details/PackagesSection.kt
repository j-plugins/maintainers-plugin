package com.github.xepozz.maintainers.toolWindow.details

import com.github.xepozz.maintainers.MaintainersBundle
import com.github.xepozz.maintainers.model.PackageInfo
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JPanel

class PackagesSection(private val onPackageSelected: (String) -> Unit) : JPanel() {
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

        val header = JBLabel(MaintainersBundle.message("details.section.packages")).apply {
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
            val packagePanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
                add(JBLabel("• ").apply {
                    border = JBUI.Borders.empty(2, 0)
                })
                add(HyperlinkLabel(pkg.name).apply {
                    addHyperlinkListener { onPackageSelected(pkg.name) }
                    border = JBUI.Borders.empty(2, 0)
                })
                add(JBLabel(" (${pkg.version})").apply {
                    foreground = JBColor.GRAY
                    border = JBUI.Borders.empty(2, 0)
                })
            }
            add(packagePanel)
        }

        if (!isExpanded && currentPackages.size > 5) {
            val moreLabel = HyperlinkLabel(MaintainersBundle.message("details.packages.more", currentPackages.size - 5)).apply {
                alignmentX = Component.LEFT_ALIGNMENT
                addHyperlinkListener {
                    isExpanded = true
                    render()
                    revalidate()
                    repaint()
                }
            }
            add(moreLabel)
        }

        revalidate()
        repaint()
    }
}
