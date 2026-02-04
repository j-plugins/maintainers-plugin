package com.github.xepozz.maintainers.toolWindow

import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.services.MaintainersService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.ide.BrowserUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

class MaintainersToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = MaintainersToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(toolWindowPanel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}

class MaintainersToolWindowPanel(private val project: Project) : OnePixelSplitter(false, 0.7f) {
    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Packages", "Funding"), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JBTable(tableModel)
    private val detailsPanel = MaintainerDetailsPanel()
    private var maintainers: List<Maintainer> = emptyList()

    init {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.selectionModel.addListSelectionListener {
            val selectedRow = table.selectedRow
            if (selectedRow >= 0 && selectedRow < maintainers.size) {
                detailsPanel.update(maintainers[selectedRow])
            }
        }

        firstComponent = JBScrollPane(table)
        secondComponent = detailsPanel
        refresh()
    }

    private fun refresh() {
        val data = MaintainerProvider.getAggregatedMaintainers(project)
        maintainers = data.keys.toList()
        tableModel.rowCount = 0
        maintainers.forEach { maintainer ->
            val count = data[maintainer] ?: 0
            tableModel.addRow(arrayOf<Any>(maintainer.name, count, if (maintainer.funding.isNotEmpty()) "Yes" else "No"))
        }
    }
}

class MaintainerDetailsPanel : JBScrollPane() {
    private val rootPanel = JBPanel<JBPanel<*>>().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(10)
    }

    init {
        setViewportView(rootPanel)
    }

    fun update(maintainer: Maintainer?) {
        rootPanel.removeAll()
        if (maintainer == null) {
            rootPanel.add(JBLabel("Select a maintainer to see details"))
        } else {
            rootPanel.add(JBLabel("Name: ${maintainer.name}").apply {
                font = font.deriveFont(java.awt.Font.BOLD)
            })
            maintainer.email?.let { rootPanel.add(JBLabel("Email: $it")) }
            maintainer.url?.let { url ->
                rootPanel.add(createHyperlinkLabel("Profile: $url", url))
            }

            if (maintainer.funding.isNotEmpty()) {
                rootPanel.add(JBUI.Panels.simplePanel().apply {
                    border = JBUI.Borders.emptyTop(10)
                    addToLeft(JBLabel("Sponsorship:").apply {
                        font = font.deriveFont(java.awt.Font.BOLD)
                    })
                })
                maintainer.funding.forEach { funding ->
                    rootPanel.add(createHyperlinkLabel("${funding.type}: ${funding.url}", funding.url))
                }
            }
        }
        rootPanel.revalidate()
        rootPanel.repaint()
    }

    private fun createHyperlinkLabel(text: String, url: String): JComponent {
        return JBLabel("<html><a href=''>$text</a></html>").apply {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    BrowserUtil.browse(url)
                }
            })
        }
    }
}
