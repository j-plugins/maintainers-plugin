package com.github.xepozz.maintainers.toolWindow

import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.toolWindow.details.MaintainerDetailsPanel
import com.github.xepozz.maintainers.toolWindow.tree.MaintainersTreeCellRenderer
import com.github.xepozz.maintainers.toolWindow.tree.MaintainersTreeModel
import com.github.xepozz.maintainers.toolWindow.tree.MaintainersTreeNode
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MaintainersToolWindowPanel(private val project: Project) : SimpleToolWindowPanel(true, true) {
    private val treeModel = MaintainersTreeModel()
    private val tree = Tree(treeModel)
    private val detailsPanel = MaintainerDetailsPanel()
    private val statusLabel = JBLabel().apply {
        foreground = JBColor.GRAY
        border = JBUI.Borders.empty(4, 8)
    }
    private val searchField = SearchTextField()
    private var allMaintainerMap: Map<Maintainer, List<Dependency>> = emptyMap()

    init {
        setupTree()
        setupToolbar()
        
        val splitter = JBSplitter(false, 0.35f)
        splitter.firstComponent = JBScrollPane(tree)
        splitter.secondComponent = detailsPanel
        splitter.dividerWidth = 1

        val mainContentPanel = JPanel(BorderLayout())
        mainContentPanel.add(splitter, BorderLayout.CENTER)
        mainContentPanel.add(statusLabel, BorderLayout.SOUTH)
        
        setContent(mainContentPanel)

        refresh()
    }

    private fun setupTree() {
        tree.cellRenderer = MaintainersTreeCellRenderer()
        tree.isRootVisible = false
        tree.selectionModel.addTreeSelectionListener {
            val node = tree.lastSelectedPathComponent as? MaintainersTreeNode
            val userObject = node?.userObject
            if (userObject is Maintainer) {
                val aggregated = allMaintainerMap.keys.find { it.name == userObject.name } ?: userObject
                detailsPanel.updateMaintainer(aggregated)
            } else if (userObject is Dependency) {
                val maintainers = userObject.maintainers.map { author ->
                    allMaintainerMap.keys.find { it.name == author.name } ?: author
                }
                detailsPanel.updateMaintainers(maintainers)
            } else {
                detailsPanel.updateMaintainer(null)
            }
        }
    }

    private fun setupToolbar() {
        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction("Refresh", "Refresh maintainers list", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) = refresh()
            })
            add(object : AnAction("Settings", "Settings", AllIcons.General.GearPlain) {
                override fun actionPerformed(e: AnActionEvent) {
                    // Settings action
                }
            })
        }

        val actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)
        actionToolbar.targetComponent = this

        val toolbarPanel = JBUI.Panels.simplePanel(searchField)
            .addToRight(actionToolbar.component)
            .withBorder(JBUI.Borders.empty(2, 5))

        setToolbar(toolbarPanel)

        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = applyFilter()
            override fun removeUpdate(e: DocumentEvent?) = applyFilter()
            override fun changedUpdate(e: DocumentEvent?) = applyFilter()
        })
    }

    private fun refresh() {
        allMaintainerMap = MaintainerProvider.getAggregatedMaintainers(project)
        applyFilter()
    }

    private fun applyFilter() {
        val state = TreeState.createOn(tree)
        val filter = searchField.text.lowercase()
        val filteredMap = if (filter.isEmpty()) {
            allMaintainerMap
        } else {
            allMaintainerMap.filter { (maintainer, dependencies) ->
                maintainer.name.lowercase().contains(filter) || 
                        dependencies.any { it.name.lowercase().contains(filter) }
            }
        }
        
        treeModel.updateData(filteredMap)
        state.applyTo(tree)
        
        val maintainersCount = filteredMap.size
        val packagesCount = filteredMap.values.flatten().distinctBy { it.name }.size
        val fundingCount = filteredMap.keys.count { it.fundingLinks.isNotEmpty() }
        
        statusLabel.text = "$maintainersCount maintainers • $packagesCount packages • $fundingCount with funding"
    }
}
