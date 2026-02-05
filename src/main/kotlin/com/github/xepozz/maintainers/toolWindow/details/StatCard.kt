package com.github.xepozz.maintainers.toolWindow.details

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.BoxLayout

class StatCard(value: String, label: String) : JBPanel<StatCard>() {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.namedColor("Borders.color", JBColor.LIGHT_GRAY), 1),
            JBUI.Borders.empty(8, 12)
        )
        isOpaque = false

        add(JBLabel(value).apply {
            font = JBFont.label().biggerOn(6f).asBold()
            alignmentX = Component.CENTER_ALIGNMENT
        })
        add(JBLabel(label).apply {
            font = JBFont.small()
            foreground = JBColor.GRAY
            alignmentX = Component.CENTER_ALIGNMENT
        })
    }
}
