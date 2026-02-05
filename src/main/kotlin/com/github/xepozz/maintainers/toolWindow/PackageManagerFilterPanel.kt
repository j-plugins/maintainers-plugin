package com.github.xepozz.maintainers.toolWindow

import com.github.xepozz.maintainers.model.PackageManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionButtonLook
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JPanel

class PackageManagerFilterPanel(
    availableManagers: List<PackageManager>,
    private val onFilterChange: (PackageManager, Boolean) -> Unit
) : JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)) {

    private val buttons = mutableMapOf<PackageManager, ActionButton>()

    init {
        isOpaque = false
        border = JBUI.Borders.empty()
        availableManagers.forEach { manager ->
            val presentation = Presentation(manager.name).apply {
                isEnabled = true
                icon = manager.icon
                Toggleable.setSelected(this, false)
            }
            val button = ActionButton(
                object : AnAction(manager.name), Toggleable {
                    override fun actionPerformed(e: AnActionEvent) {
                        val selected = !Toggleable.isSelected(presentation)
                        Toggleable.setSelected(presentation, selected)
                        onFilterChange(manager, selected)
                    }

                    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
                },
                presentation,
                ActionPlaces.TOOLBAR,
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
            ).apply {
                setLook(ActionButtonLook.SYSTEM_LOOK)
            }
            buttons[manager] = button
            add(button)
        }
    }

    fun updateState(selectedManagers: Set<PackageManager>) {
        buttons.forEach { (manager, button) ->
            Toggleable.setSelected(button.presentation, selectedManagers.contains(manager))
        }
        repaint()
    }
}
