package com.github.xepozz.maintainers.toolWindow.details

import com.github.xepozz.maintainers.MaintainersBundle
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.providers.composer.ComposerPackageManager
import com.github.xepozz.maintainers.providers.go.GoPackageManager
import com.github.xepozz.maintainers.providers.npm.NpmPackageManager
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.util.maximumWidth
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Rectangle
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.ScrollPaneConstants
import javax.swing.Scrollable
import javax.swing.SwingConstants

class PackageDetailsPanel : JBScrollPane() {
    private var onMaintainerSelected: ((Maintainer) -> Unit)? = null

    private val rootPanel = object : JBPanel<JBPanel<*>>(), Scrollable {
        init {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(20)
            isOpaque = false
        }

        override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
        override fun getScrollableUnitIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int = 10
        override fun getScrollableBlockIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int = 100
        override fun getScrollableTracksViewportWidth(): Boolean = true
        override fun getScrollableTracksViewportHeight(): Boolean = false
    }

    init {
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        val wrapper = JBUI.Panels.simplePanel()
        wrapper.addToTop(rootPanel)
        setViewportView(wrapper)
        border = JBUI.Borders.empty()
    }

    fun setOnMaintainerSelected(callback: (Maintainer) -> Unit) {
        this.onMaintainerSelected = callback
    }

    fun updatePackage(dependency: Dependency, aggregatedMaintainers: Map<Maintainer, List<Dependency>>) {
        rootPanel.removeAll()

        rootPanel.add(createHeaderSection(dependency))
        rootPanel.add(createSeparator())

        rootPanel.add(createPackageInfoSection(dependency))
        rootPanel.add(createSeparator())

        val linksSection = createLinksSection(dependency, aggregatedMaintainers)
        if (linksSection != null) {
            rootPanel.add(linksSection)
            rootPanel.add(createSeparator())
        }

        val enrichedMaintainers = dependency.maintainers.map { author ->
            aggregatedMaintainers.keys.find { it.name == author.name } ?: author
        }
        val authorsSection = createAuthorsSection(enrichedMaintainers)
        if (authorsSection != null) {
            rootPanel.add(authorsSection)
        }

        rootPanel.revalidate()
        rootPanel.repaint()
    }

    private fun createHeaderSection(dependency: Dependency): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(10)

            // Left side: Icon + Name + Type
            val leftPanel = JPanel(BorderLayout(10, 0)).apply {
                isOpaque = false

                // Icon
                add(JBLabel(IconUtil.scale(dependency.source.icon, null, 2f)), BorderLayout.WEST)

                // Name + Type vertical stack
                val textPanel = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false

                    add(JBLabel(dependency.name).apply {
                        font = JBFont.label().biggerOn(4f).asBold()
                        alignmentX = Component.LEFT_ALIGNMENT
                    })

                    add(JBLabel(dependency.source.name).apply {
                        foreground = com.intellij.util.ui.UIUtil.getContextHelpForeground()
                        alignmentX = Component.LEFT_ALIGNMENT
                    })
                }
                add(textPanel, BorderLayout.CENTER)
            }

            // Right side: Package Manager Button
            val packageManagerButton = JButton(getPackageManagerButtonText(dependency.source)).apply {
                addActionListener {
                    getPackageManagerUrl(dependency)?.let { BrowserUtil.browse(it) }
                }
            }

            add(leftPanel, BorderLayout.CENTER)
            add(packageManagerButton, BorderLayout.EAST)
        }
    }

    private fun getPackageManagerButtonText(packageManager: com.github.xepozz.maintainers.model.PackageManager): String {
        return when (packageManager) {
            ComposerPackageManager -> MaintainersBundle.message("package.details.button.packagist")
            NpmPackageManager -> MaintainersBundle.message("package.details.button.npm")
            GoPackageManager -> MaintainersBundle.message("package.details.button.pkggodev")
            else -> MaintainersBundle.message("package.details.button.view")
        }
    }

    private fun getPackageManagerUrl(dependency: Dependency): String? {
        return when (dependency.source) {
            ComposerPackageManager -> "https://packagist.org/packages/${dependency.name}"
            NpmPackageManager -> "https://www.npmjs.com/package/${dependency.name}"
            GoPackageManager -> "https://pkg.go.dev/${dependency.name}@${dependency.version}"
            else -> null
        }
    }

    private fun createPackageInfoSection(dependency: Dependency): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyTop(10)
        }

        panel.add(createSectionHeader(MaintainersBundle.message("package.details.section.info")))
        panel.add(Box.createVerticalStrut(8))
        panel.add(createInfoRow(MaintainersBundle.message("package.details.info.version"), dependency.version))
        dependency.url?.let { url ->
            // If it's a URL, we might want to show it as repository if it looks like one
            if (url.contains("github.com") || url.contains("gitlab.com") || url.contains("bitbucket.org")) {
                val repoName = url.substringAfter("://").substringAfter("/")
                panel.add(createInfoRow(MaintainersBundle.message("package.details.info.repository"), repoName.ifBlank { url }))
            }
        }

        return panel
    }

    private fun createLinksSection(dependency: Dependency, aggregatedMaintainers: Map<Maintainer, List<Dependency>>): JPanel? {
        val links = mutableListOf<Pair<String, String>>()

        // Repository link
        dependency.url?.let { url ->
            links.add(MaintainersBundle.message("package.details.link.repository") to url)
        }

        // Homepage from maintainers
        dependency.maintainers.firstNotNullOfOrNull { author ->
            aggregatedMaintainers.keys.find { it.name == author.name }?.homepage ?: author.homepage
        }?.let { url ->
            if (url != dependency.url && links.none { it.second == url }) {
                links.add(MaintainersBundle.message("package.details.link.homepage") to url)
            }
        }

        if (links.isEmpty()) {
            return null
        }

        val section = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyTop(10)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        val header = JBLabel(MaintainersBundle.message("package.details.section.links")).apply {
            font = JBFont.label().asBold()
            foreground = JBColor.namedColor("Label.disabledForeground", JBColor.GRAY)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        section.add(header)
        section.add(Box.createVerticalStrut(8))

        links.forEach { (label, url) ->
            section.add(createLinkRow(label, url))
        }

        return section
    }

    private fun createAuthorsSection(maintainers: List<Maintainer>): JPanel? {
        if (maintainers.isEmpty()) {
            return null
        }

        val section = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyTop(10)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        // Section header
        val header = JBLabel(
            MaintainersBundle.message("package.details.section.authors", maintainers.size)
        ).apply {
            font = JBFont.label().asBold()
            foreground = JBColor.namedColor("Label.disabledForeground", JBColor.GRAY)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        section.add(header)
        section.add(Box.createVerticalStrut(8))

        // Author cards
        maintainers.forEach { maintainer ->
            section.add(createAuthorCard(maintainer))
            section.add(Box.createVerticalStrut(8))
        }

        return section
    }

    private fun createAuthorCard(maintainer: Maintainer): JPanel {
        val panel = JPanel(BorderLayout(10, 0)).apply {
            isOpaque = true
            background = JBColor.namedColor("WelcomeScreen.Details.background", JBColor(0xF7F8FA, 0x2B2D30))
            border = JBUI.Borders.empty(8)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 60)
        }

        val iconLabel = JBLabel(AllIcons.General.User)
        val githubUsername = maintainer.github
        val iconUrl = maintainer.icon
        if (githubUsername != null) {
            val cachedIcon = AvatarLoader.getIconIfLoaded(githubUsername, 32)
            if (cachedIcon != null) {
                iconLabel.icon = cachedIcon
            } else {
                AvatarLoader.loadIcon(githubUsername, 32) { icon ->
                    iconLabel.icon = icon
                }
            }
        } else if (iconUrl != null) {
            val cachedIcon = AvatarLoader.getIconByUrlIfLoaded(iconUrl, 32)
            if (cachedIcon != null) {
                iconLabel.icon = cachedIcon
            } else {
                AvatarLoader.loadIconByUrl(iconUrl, 32) { icon ->
                    iconLabel.icon = icon
                }
            }
        }
        panel.add(iconLabel, BorderLayout.WEST)

        val infoPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            val nameLabel = HyperlinkLabel(maintainer.name).apply {
                font = JBFont.label().asBold()
                alignmentX = Component.LEFT_ALIGNMENT
                addHyperlinkListener { onMaintainerSelected?.invoke(maintainer) }
            }
            add(nameLabel)
            
            maintainer.email?.let { email ->
                add(JBLabel(email).apply {
                    font = JBFont.small()
                    foreground = JBColor.GRAY
                    alignmentX = Component.LEFT_ALIGNMENT
                })
            }
        }
        panel.add(infoPanel, BorderLayout.CENTER)

        if (maintainer.fundingLinks.isNotEmpty()) {
            val fundingButton = JButton(AllIcons.Nodes.Favorite).apply {
                toolTipText = "Sponsor"
                isContentAreaFilled = false
                isBorderPainted = false
                addActionListener {
                    // Open first funding link for simplicity in the card
                    BrowserUtil.browse(maintainer.fundingLinks.first().url)
                }
            }
            panel.add(fundingButton, BorderLayout.EAST)
        }

        return panel
    }

    private fun createSectionHeader(text: String): JBLabel {
        return JBLabel(text).apply {
            font = JBFont.label().asBold()
            foreground = JBColor.namedColor("Label.disabledForeground", JBColor.GRAY)
            alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    private fun createInfoRow(label: String, value: String): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(JBLabel(label).apply {
                foreground = JBColor.GRAY
            }, BorderLayout.WEST)
            add(JBLabel(value).apply {
                horizontalAlignment = SwingConstants.RIGHT
            }, BorderLayout.CENTER)
            maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        }
    }

    private fun createLinkRow(text: String, url: String): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(JBLabel(AllIcons.Ide.External_link_arrow))
            add(HyperlinkLabel(text).apply {
                addHyperlinkListener {
                    val fullUrl = if (!url.contains("://")) "https://$url" else url
                    BrowserUtil.browse(fullUrl)
                }
            })
        }
    }

    private fun createSeparator(): JSeparator {
        return JSeparator().apply {
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 1)
        }
    }
}
