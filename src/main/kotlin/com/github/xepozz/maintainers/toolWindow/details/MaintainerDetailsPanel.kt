package com.github.xepozz.maintainers.toolWindow.details

import com.github.xepozz.maintainers.model.Maintainer
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JSeparator

class MaintainerDetailsPanel : JBScrollPane() {
    private val rootPanel = JBPanel<JBPanel<*>>().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(20)
        isOpaque = false
    }

    init {
        val wrapper = JBUI.Panels.simplePanel()
        wrapper.addToTop(rootPanel)
        setViewportView(wrapper)
        border = JBUI.Borders.empty()
    }

    fun updateMaintainers(maintainers: List<Maintainer>) {
        rootPanel.removeAll()
        if (maintainers.isEmpty()) {
            rootPanel.add(JBLabel("Select a package or maintainer to see details").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                foreground = JBColor.GRAY
            })
        } else {
            maintainers.forEachIndexed { index, maintainer ->
                if (index > 0) {
                    rootPanel.add(createMainSeparator())
                }
                addMaintainerSection(maintainer)
            }
        }
        rootPanel.revalidate()
        rootPanel.repaint()
    }

    fun updateMaintainer(maintainer: Maintainer?) {
        updateMaintainers(if (maintainer == null) emptyList() else listOf(maintainer))
    }

    private fun addMaintainerSection(maintainer: Maintainer) {
        // Header
        val headerPanel = JPanel(BorderLayout(10, 0)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val iconLabel = JBLabel(IconUtil.scale(AllIcons.General.User, null, 2f))
        headerPanel.add(iconLabel, BorderLayout.WEST)

        val nameSubtitlePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(JBLabel(maintainer.name).apply {
                font = JBFont.label().biggerOn(4f).asBold()
                alignmentX = Component.LEFT_ALIGNMENT
            })
            add(JBLabel("Maintainer of ${maintainer.packages.size} packages in project").apply {
                foreground = JBColor.GRAY
                alignmentX = Component.LEFT_ALIGNMENT
            })
        }
        headerPanel.add(nameSubtitlePanel, BorderLayout.CENTER)
        rootPanel.add(headerPanel)

        // Contact
        val contactPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyTop(10)
        }
        maintainer.email?.let { email ->
            contactPanel.add(createContactLine(AllIcons.Ide.External_link_arrow, email, "mailto:$email"))
        }
        maintainer.homepage?.let { homepage ->
            contactPanel.add(createContactLine(AllIcons.Ide.External_link_arrow, homepage, homepage))
        }
        maintainer.github?.let { github ->
            contactPanel.add(createContactLine(AllIcons.Ide.External_link_arrow, "github.com/$github", "https://github.com/$github"))
        }
        if (contactPanel.componentCount > 0) {
            rootPanel.add(contactPanel)
        }

        // Separator
        rootPanel.add(createSeparator())

        // Funding
        if (maintainer.fundingLinks.isNotEmpty()) {
            val fundingSection = FundingSection()
            fundingSection.update(maintainer.fundingLinks)
            rootPanel.add(fundingSection)
            rootPanel.add(createSeparator())
        }

        // Packages
        if (maintainer.packages.isNotEmpty()) {
            val packagesSection = PackagesSection()
            packagesSection.update(maintainer.packages)
            rootPanel.add(packagesSection)
        }
    }

    private fun createSeparator(): JSeparator {
        return JSeparator().apply {
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 1)
        }
    }

    private fun createMainSeparator(): JPanel {
        return JPanel().apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.empty(15, 0)
            layout = BorderLayout()
            add(JSeparator().apply {
                foreground = JBColor.namedColor("Component.infoForeground", JBColor.LIGHT_GRAY)
            })
        }
    }

    private fun createContactLine(icon: javax.swing.Icon, text: String, url: String): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(JBLabel(icon))
            add(HyperlinkLabel(text).apply {
                addHyperlinkListener { BrowserUtil.browse(url) }
            })
        }
    }
}
