package com.github.xepozz.maintainers.toolWindow.details

import com.github.xepozz.maintainers.model.FundingSource
import com.intellij.ide.BrowserUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.FlowLayout
import java.util.*
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

        val header = JBLabel("FUNDING").apply {
            font = JBFont.label().asBold()
            foreground = JBColor.namedColor("Label.disabledForeground", JBColor.GRAY)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        add(header)

        val buttonsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 5)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
        }
        
        fundingLinks.forEach { link ->
            val button = JButton(link.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }).apply {
                addActionListener { BrowserUtil.browse(link.url) }
            }
            buttonsPanel.add(button)
        }
        add(buttonsPanel)
        
        revalidate()
        repaint()
    }
}
