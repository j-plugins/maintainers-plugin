package com.github.xepozz.maintainers.toolWindow

import com.github.xepozz.maintainers.MaintainersBundle
import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.*
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
import com.intellij.util.ui.JBFont
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
    private val closeActionToolbar = ActionManager.getInstance().createActionToolbar(
        "DetailsHeader",
        DefaultActionGroup(object : AnAction("Close", "Close details view", AllIcons.Actions.Close) {
            override fun actionPerformed(e: AnActionEvent) {
                tree.clearSelection()
            }
        }),
        true
    )
    private val detailsHeader = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.empty(4, 8)
        add(JBLabel("Details").apply {
            font = JBFont.label().asBold()
            foreground = JBColor.GRAY
        }, BorderLayout.WEST)

        closeActionToolbar.targetComponent = this
        add(closeActionToolbar.component, BorderLayout.EAST)
    }
    private val detailsContainer = JBUI.Panels.simplePanel(detailsPanel).addToTop(detailsHeader)
    private val splitter = JBSplitter(false, 0.35f).apply {
        firstComponent = JBScrollPane(tree)
        secondComponent = detailsContainer
        dividerWidth = 1
    }
    private var detailsVisible = true
    private val statusLabel = JBLabel().apply {
        foreground = JBColor.GRAY
        border = JBUI.Borders.empty(4, 8)
    }
    private val searchField = SearchTextField()
    private var aggregatedData: AggregatedData = AggregatedData(emptyMap(), emptyList())
    private var currentStats: MaintainersStats? = null

    init {
        setupTree()
        setupToolbar()
        
        detailsPanel.setOnPackageSelected { packageName ->
            selectDependency(packageName)
        }

        val mainContentPanel = JPanel(BorderLayout())
        mainContentPanel.add(splitter, BorderLayout.CENTER)
        mainContentPanel.add(statusLabel, BorderLayout.SOUTH)
        
        setContent(mainContentPanel)

        refresh()
    }

    private fun setDetailsVisible(visible: Boolean) {
        if (detailsVisible == visible) return
        detailsVisible = visible
        splitter.secondComponent = if (visible) detailsContainer else null
        splitter.revalidate()
        splitter.repaint()
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
                setDetailsVisible(true)
                closeActionToolbar.component.isVisible = true
                val aggregated = aggregatedData.maintainerMap.keys.find { it.name == userObject.name } ?: userObject
                detailsPanel.updateMaintainer(aggregated)
            } else if (userObject is Dependency) {
                setDetailsVisible(true)
                closeActionToolbar.component.isVisible = true
                val maintainers = userObject.maintainers.map { author ->
                    aggregatedData.maintainerMap.keys.find { it.name == author.name } ?: author
                }
                detailsPanel.updateMaintainers(maintainers)
            } else {
                showEmptyState()
            }
        }
    }

    private fun showEmptyState() {
        closeActionToolbar.component.isVisible = false
        val stats = currentStats ?: return
        detailsPanel.showEmptyState(
            stats,
            onFilterFunding = {
                searchField.text = "is:funding"
                applyFilter()
            },
            onMaintainerClick = { maintainer ->
                selectMaintainer(maintainer)
            }
        )
    }

    private fun selectMaintainer(maintainer: Maintainer) {
        TreeUtil.promiseSelect(tree, object : TreeVisitor {
            override fun visit(path: TreePath): TreeVisitor.Action {
                var userObject = TreeUtil.getUserObject(path.lastPathComponent)
                if (userObject is NodeDescriptor<*>) {
                    userObject = userObject.element
                }

                if (userObject is Maintainer && userObject.name == maintainer.name) {
                    return TreeVisitor.Action.INTERRUPT
                }
                return TreeVisitor.Action.CONTINUE
            }
        })
    }

    private fun setupToolbar() {
        val actionGroup = DefaultActionGroup().apply {
            add(object : AnAction(
                MaintainersBundle.message("action.refresh.text"),
                MaintainersBundle.message("action.refresh.description"),
                AllIcons.Actions.Refresh
            ) {
                override fun actionPerformed(e: AnActionEvent) = refresh()
            })
            add(object : ToggleAction(
                MaintainersBundle.message("action.show.details.text"),
                MaintainersBundle.message("action.show.details.description"),
                AllIcons.Actions.Preview
            ) {
                override fun isSelected(e: AnActionEvent): Boolean = detailsVisible
                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    setDetailsVisible(state)
                }
            })
            add(Separator())
            add(object : AnAction(
                MaintainersBundle.message("action.settings.text"),
                MaintainersBundle.message("action.settings.description"),
                AllIcons.General.GearPlain
            ) {
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
        aggregatedData = MaintainerProvider.getAggregatedData(project)
        
        val maintainerMap = aggregatedData.maintainerMap
        val maintainersCount = maintainerMap.size
        val packagesCount = aggregatedData.allDependencies.distinctBy { it.name }.size
        val fundingCount = maintainerMap.keys.count { it.fundingLinks.isNotEmpty() }
        val topMaintainers = maintainerMap.keys
            .sortedByDescending { it.packages.size }
            .take(3)
        
        currentStats = MaintainersStats(maintainersCount, packagesCount, fundingCount, topMaintainers)

        treeStructure.updateData(maintainerMap, aggregatedData.allDependencies)
        structureModel.invalidateAsync()
        applyFilter()

        if (tree.selectionCount == 0) {
            showEmptyState()
        }
    }

    private fun applyFilter() {
        val filterText = searchField.text.lowercase()
        treeStructure.setFilter(filterText)
        structureModel.invalidateAsync()
        
        val filteredMap = if (filterText.isEmpty()) {
            aggregatedData.maintainerMap
        } else if (filterText == "is:funding") {
            aggregatedData.maintainerMap.filter { it.key.fundingLinks.isNotEmpty() }
        } else {
            aggregatedData.maintainerMap.filter { (maintainer, dependencies) ->
                maintainer.name.lowercase().contains(filterText) || 
                        dependencies.any { it.name.lowercase().contains(filterText) }
            }
        }

        val filteredDependencies = if (filterText.isEmpty()) {
            aggregatedData.allDependencies
        } else if (filterText == "is:funding") {
            aggregatedData.allDependencies.filter { it.maintainers.any { m -> m.fundingLinks.isNotEmpty() } }
        } else {
            aggregatedData.allDependencies.filter { dependency ->
                dependency.name.lowercase().contains(filterText) ||
                        dependency.maintainers.any { it.name.lowercase().contains(filterText) }
            }
        }
        
        val maintainersCount = filteredMap.size
        val packagesCount = filteredDependencies.distinctBy { it.name }.size
        val fundingCount = filteredMap.keys.count { it.fundingLinks.isNotEmpty() }
        
        statusLabel.text = MaintainersBundle.message("toolwindow.status", maintainersCount, packagesCount, fundingCount)
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
