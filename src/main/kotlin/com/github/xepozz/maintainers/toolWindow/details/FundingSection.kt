package com.github.xepozz.maintainers.toolWindow.details

import com.github.xepozz.maintainers.MaintainersBundle
import com.github.xepozz.maintainers.model.FundingSource
import com.intellij.ide.BrowserUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.util.*
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class FundingSection : JPanel() {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.emptyTop(10)
    }

    fun update(fundingLinks: List<FundingSource>) {
        removeAll()
        if (fundingLinks.isEmpty()) {
            isVisible = false
            return
        }
        isVisible = true

        val header = JBLabel(MaintainersBundle.message("details.section.funding")).apply {
            font = JBFont.label().asBold()
            foreground = JBColor.namedColor("Label.disabledForeground", JBColor.GRAY)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        add(header)

        val buttonsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            border = JBUI.Borders.empty(5, 0)
        }
        
        fundingLinks.forEachIndexed { index, link ->
            val button = JButton(link.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }).apply {
                addActionListener { BrowserUtil.browse(link.url) }
            }
            buttonsPanel.add(button)
            if (index < fundingLinks.size - 1) {
                buttonsPanel.add(Box.createRigidArea(Dimension(5, 0)))
            }
        }
        
        val scrollPane = JBScrollPane(buttonsPanel).apply {
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_NEVER
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            border = JBUI.Borders.empty()
            isOpaque = false
            viewport.isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
        }
        add(scrollPane)
        
        revalidate()
        repaint()
    }
}
