package com.github.xepozz.maintainers.toolWindow

import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.services.MaintainersService
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.ide.BrowserUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class MaintainersToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = MaintainersToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(toolWindowPanel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}

class MaintainersToolWindowPanel(private val project: Project) : SimpleToolWindowPanel(true, true) {
    private val splitter = OnePixelSplitter(false, 0.7f)
    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Packages"), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                1 -> java.lang.Integer::class.java
                else -> java.lang.String::class.java
            }
        }
    }
    private val table = JBTable(tableModel)
    private val detailsPanel = MaintainerDetailsPanel()
    private val packageFilter = ComboBox<String>()
    private val maintainerFilter = ComboBox<String>()

    private var allMaintainerPackages: Map<Maintainer, List<Dependency>> = emptyMap()
    private var currentMaintainers: List<Maintainer> = emptyList()

    init {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
        table.autoCreateRowSorter = true
        table.selectionModel.addListSelectionListener {
            val viewRow = table.selectedRow
            if (viewRow >= 0) {
                val modelRow = table.convertRowIndexToModel(viewRow)
                if (modelRow >= 0 && modelRow < currentMaintainers.size) {
                    val maintainer = currentMaintainers[modelRow]
                    detailsPanel.update(maintainer, allMaintainerPackages[maintainer] ?: emptyList())
                }
            }
        }

        table.columnModel.getColumn(0).preferredWidth = 400
        table.columnModel.getColumn(0).cellRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
                component.icon = AllIcons.General.User
                return component
            }
        }
        table.columnModel.getColumn(1).preferredWidth = 100

        packageFilter.addActionListener { applyFilters() }
        maintainerFilter.addActionListener { applyFilters() }

        ComboboxSpeedSearch.installSpeedSearch(packageFilter) { it }
        ComboboxSpeedSearch.installSpeedSearch(maintainerFilter) { it }

        val toolbarPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 10, 0))
        toolbarPanel.add(JBLabel("Package:"))
        toolbarPanel.add(packageFilter)
        toolbarPanel.add(JBLabel("Maintainer:"))
        toolbarPanel.add(maintainerFilter)

        setToolbar(toolbarPanel)

        splitter.firstComponent = JBScrollPane(table)
        splitter.setResizeEnabled(true)
        splitter.secondComponent = detailsPanel
        setContent(splitter)

        refresh()
    }

    private fun refresh() {
        allMaintainerPackages = MaintainerProvider.getAggregatedMaintainers(project)

        val packages = allMaintainerPackages.values.flatten().map { it.name }.distinct().sorted()
        val maintainerNames = allMaintainerPackages.keys.map { it.name }.distinct().sorted()

        updateFilterModel(packageFilter, packages)
        updateFilterModel(maintainerFilter, maintainerNames)

        applyFilters()
    }

    private fun updateFilterModel(comboBox: ComboBox<String>, items: List<String>) {
        val model = DefaultComboBoxModel<String>()
        model.addElement("All")
        items.forEach { model.addElement(it) }
        comboBox.model = model
    }

    private fun applyFilters() {
        val selectedPackage = packageFilter.selectedItem as? String ?: "All"
        val selectedMaintainer = maintainerFilter.selectedItem as? String ?: "All"

        val filteredData = allMaintainerPackages.filter { (maintainer, pkgs) ->
            val packageMatch = selectedPackage == "All" || pkgs.any { it.name == selectedPackage }
            val maintainerMatch = selectedMaintainer == "All" || maintainer.name == selectedMaintainer
            packageMatch && maintainerMatch
        }

        tableModel.rowCount = 0
        currentMaintainers = filteredData.keys.toList()
        currentMaintainers.forEach { maintainer ->
            val pkgs = filteredData[maintainer] ?: emptyList()
            tableModel.addRow(arrayOf<Any>(maintainer.name, pkgs.size))
        }
    }
}

class MaintainerDetailsPanel : JBScrollPane() {
    private val rootPanel = JBPanel<JBPanel<*>>().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(10)
    }

    init {
        val wrapper = JBUI.Panels.simplePanel()
        wrapper.addToTop(rootPanel)
        setViewportView(wrapper)
    }

    fun update(maintainer: Maintainer?, packages: List<Dependency> = emptyList()) {
        rootPanel.removeAll()
        if (maintainer == null) {
            rootPanel.add(JBLabel("Select a maintainer to see details").apply {
                alignmentX = Component.LEFT_ALIGNMENT
            })
        } else {
            rootPanel.add(JBLabel(maintainer.name).apply {
                font = font.deriveFont(java.awt.Font.BOLD)
                alignmentX = Component.LEFT_ALIGNMENT
                icon = AllIcons.General.User
            })
            maintainer.email?.let {
                rootPanel.add(JBLabel("Email: $it").apply {
                    alignmentX = Component.LEFT_ALIGNMENT
                })
            }
            maintainer.url?.let { url ->
                rootPanel.add(createHyperlinkLabel("Profile: $url", url).apply {
                    alignmentX = Component.LEFT_ALIGNMENT
                })
            }

            if (packages.isNotEmpty()) {
                rootPanel.add(JBLabel("Packages:").apply {
                    font = font.deriveFont(java.awt.Font.BOLD)
                    border = JBUI.Borders.emptyTop(10)
                    alignmentX = Component.LEFT_ALIGNMENT
                })
                packages.forEach { pkg ->
                    val pkgText = pkg.name + (if (pkg.version.isNotEmpty()) " (${pkg.version})" else "")
                    if (pkg.url != null) {
                        rootPanel.add(createHyperlinkLabel(pkgText, pkg.url).apply {
                            alignmentX = Component.LEFT_ALIGNMENT
                        })
                    } else {
                        rootPanel.add(JBLabel(pkgText).apply {
                            alignmentX = Component.LEFT_ALIGNMENT
                        })
                    }
                }
            }

            if (maintainer.funding.isNotEmpty()) {
                rootPanel.add(JBLabel("Sponsorship:").apply {
                    font = font.deriveFont(java.awt.Font.BOLD)
                    border = JBUI.Borders.emptyTop(10)
                    alignmentX = Component.LEFT_ALIGNMENT
                })
                maintainer.funding.forEach { funding ->
                    rootPanel.add(createHyperlinkLabel("${funding.type}: ${funding.url}", funding.url).apply {
                        alignmentX = Component.LEFT_ALIGNMENT
                    })
                }
            }
        }
        rootPanel.revalidate()
        rootPanel.repaint()
    }

    private fun createHyperlinkLabel(text: String, url: String): JComponent {
        return JBLabel("<html><a href=''>$text</a></html>").apply {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            alignmentX = Component.LEFT_ALIGNMENT
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    BrowserUtil.browse(url)
                }
            })
        }
    }
}
