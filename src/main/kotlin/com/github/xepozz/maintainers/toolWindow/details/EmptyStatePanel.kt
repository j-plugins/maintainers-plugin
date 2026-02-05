package com.github.xepozz.maintainers.toolWindow.details

import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.model.MaintainersStats
import com.intellij.icons.AllIcons
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants

class EmptyStatePanel(
    private val stats: MaintainersStats,
    private val onFilterFunding: () -> Unit,
    private val onMaintainerClick: (Maintainer) -> Unit
) : JBPanel<EmptyStatePanel>(BorderLayout()) {

    init {
        isOpaque = false
        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(40, 20)
        }

        // Header Section
        content.add(JBLabel(IconUtil.scale(AllIcons.General.User, null, 3f)).apply {
            alignmentX = Component.CENTER_ALIGNMENT
        })
        content.add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.emptyTop(20)
            alignmentX = Component.CENTER_ALIGNMENT
            add(JBLabel("Your project depends on").apply {
                foreground = JBColor.GRAY
                alignmentX = Component.CENTER_ALIGNMENT
            })
            add(JBLabel("${stats.maintainersCount} open source maintainers").apply {
                font = JBFont.label().biggerOn(6f).asBold()
                alignmentX = Component.CENTER_ALIGNMENT
            })
        })

        // Stats Cards Section
        val statsPanel = JPanel(FlowLayout(FlowLayout.CENTER, 16, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.empty(30, 0)
            alignmentX = Component.CENTER_ALIGNMENT
            add(StatCard(stats.maintainersCount.toString(), "maintainers"))
            add(StatCard(stats.packagesCount.toString(), "packages"))
            add(StatCard(stats.withFundingCount.toString(), "with funding"))
        }
        content.add(statsPanel)

        content.add(createSeparator())

        // Top Contributors Section
        if (stats.topMaintainers.isNotEmpty()) {
            content.add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.empty(20, 0)
                alignmentX = Component.CENTER_ALIGNMENT
                
                add(JBLabel("Top contributors:").apply {
                    font = JBFont.label().asBold()
                    alignmentX = Component.CENTER_ALIGNMENT
                })
                
                val medals = listOf("🥇", "🥈", "🥉")
                stats.topMaintainers.take(3).forEachIndexed { index, maintainer ->
                    val medal = medals.getOrElse(index) { "" }
                    val label = JBLabel("$medal ${maintainer.name} (${maintainer.packages.size})").apply {
                        cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                        alignmentX = Component.CENTER_ALIGNMENT
                        border = JBUI.Borders.emptyTop(8)
                        addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent) = onMaintainerClick(maintainer)
                            override fun mouseEntered(e: MouseEvent) {
                                foreground = JBColor.namedColor("Link.activeForeground", JBColor.BLUE)
                            }
                            override fun mouseExited(e: MouseEvent) {
                                foreground = UIUtil.getLabelForeground()
                            }
                        })
                    }
                    add(label)
                }
            })
            content.add(createSeparator())
        }

        // Funding Section
        content.add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(20, 0)
            alignmentX = Component.CENTER_ALIGNMENT
            
            add(JBLabel("💚 ${stats.withFundingCount} maintainers accept sponsorship").apply {
                alignmentX = Component.CENTER_ALIGNMENT
            })
            add(HyperlinkLabel("Show only with funding").apply {
                addHyperlinkListener { onFilterFunding() }
                alignmentX = Component.CENTER_ALIGNMENT
            })
        })

        content.add(createSeparator())

        // Hint Section
        content.add(JBLabel("← Select a maintainer to see details").apply {
            foreground = JBColor.GRAY
            border = JBUI.Borders.emptyTop(20)
            alignmentX = Component.CENTER_ALIGNMENT
        })

        add(content, BorderLayout.NORTH)
    }

    private fun createSeparator() = JSeparator(SwingConstants.HORIZONTAL).apply {
        maximumSize = java.awt.Dimension(400, 1)
        foreground = JBColor.namedColor("Borders.color", JBColor.LIGHT_GRAY)
        alignmentX = Component.CENTER_ALIGNMENT
    }
}
