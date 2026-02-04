package com.github.xepozz.maintainers.toolWindow

import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.toolWindow.details.MaintainerDetailsPanel
import com.github.xepozz.maintainers.toolWindow.tree.*
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Condition
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.FilteringTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.tree.TreeVisitor
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.TreePath

class MaintainersToolWindowPanel(private val project: Project) : SimpleToolWindowPanel(true, true), Disposable {
    private val treeStructure = MaintainersTreeStructure(project)
    private val structureModel = StructureTreeModel(treeStructure, this)
    private val asyncTreeModel = AsyncTreeModel(structureModel, this)
    private val tree = Tree(asyncTreeModel)
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
        
        detailsPanel.setOnPackageSelected { packageName ->
            selectDependency(packageName)
        }

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

    override fun dispose() {
    }

    private fun setupTree() {
        tree.cellRenderer = MaintainersTreeCellRenderer()
        tree.isRootVisible = false
        tree.selectionModel.addTreeSelectionListener {
            var userObject = TreeUtil.getUserObject(tree.lastSelectedPathComponent)
            if (userObject is NodeDescriptor<*>) {
                userObject = userObject.element
            }

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
        treeStructure.updateData(allMaintainerMap)
        structureModel.invalidateAsync()
        applyFilter()
    }

    private fun applyFilter() {
        val filterText = searchField.text.lowercase()
        treeStructure.setFilter(filterText)
        structureModel.invalidateAsync()
        
        val filteredMap = if (filterText.isEmpty()) {
            allMaintainerMap
        } else {
            allMaintainerMap.filter { (maintainer, dependencies) ->
                maintainer.name.lowercase().contains(filterText) || 
                        dependencies.any { it.name.lowercase().contains(filterText) }
            }
        }
        
        val maintainersCount = filteredMap.size
        val packagesCount = filteredMap.values.flatten().distinctBy { it.name }.size
        val fundingCount = filteredMap.keys.count { it.fundingLinks.isNotEmpty() }
        
        statusLabel.text = "$maintainersCount maintainers • $packagesCount packages • $fundingCount with funding"
    }

    private fun selectDependency(packageName: String) {
        TreeUtil.promiseSelect(tree, object : TreeVisitor {
            override fun visit(path: TreePath): TreeVisitor.Action {
                var userObject = TreeUtil.getUserObject(path.lastPathComponent)
                if (userObject is NodeDescriptor<*>) {
                    userObject = userObject.element
                }
                
                if (userObject is Dependency && userObject.name == packageName) {
                    return TreeVisitor.Action.INTERRUPT
                }
                return TreeVisitor.Action.CONTINUE
            }
        })
    }
}
