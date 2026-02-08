package com.github.xepozz.maintainers.toolWindow

import com.github.xepozz.maintainers.MaintainersBundle
import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.AggregatedData
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.model.MaintainersStats
import com.github.xepozz.maintainers.toolWindow.details.MaintainerDetailsPanel
import com.github.xepozz.maintainers.toolWindow.details.PackageDetailsPanel
import com.github.xepozz.maintainers.toolWindow.tree.MaintainersTreeCellRenderer
import com.github.xepozz.maintainers.toolWindow.tree.MaintainersTreeStructure
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.tree.TreeVisitor
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
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
    private val packageDetailsPanel = PackageDetailsPanel()
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
        dividerWidth = 2
        divider.background = JBColor.namedColor("Borders.color", JBColor.LIGHT_GRAY)
    }
    private var detailsVisible = true
    private val statusLabel = JBLabel().apply {
        foreground = JBColor.GRAY
        border = JBUI.Borders.empty(4, 8)
    }
    private val searchField = SearchTextField()
    private var filterController: SearchFilterController? = null
    private var managerPanel: PackageManagerFilterPanel? = null
    private var aggregatedData: AggregatedData = AggregatedData(emptyMap(), emptyList())
    private var currentStats: MaintainersStats? = null

    init {
        setupTree()
        setupSpeedSearch()
        setupToolbar()

        detailsPanel.setOnPackageSelected { packageName ->
            selectDependency(packageName)
        }

        packageDetailsPanel.setOnMaintainerSelected { maintainer ->
            selectMaintainer(maintainer)
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
                showMaintainerDetails()
                val aggregated = aggregatedData.maintainerMap.keys.find { it.name == userObject.name } ?: userObject
                detailsPanel.updateMaintainer(aggregated)
            } else if (userObject is Dependency) {
                setDetailsVisible(true)
                closeActionToolbar.component.isVisible = true
                showPackageDetails()
                packageDetailsPanel.updatePackage(userObject, aggregatedData.maintainerMap)
            } else {
                showEmptyState()
            }
        }
    }

    private fun setupSpeedSearch() {
        TreeUIHelper.getInstance().installTreeSpeedSearch(tree, { path ->
            var userObject = TreeUtil.getUserObject(path.lastPathComponent)
            if (userObject is NodeDescriptor<*>) {
                userObject = userObject.element
            }
            when (userObject) {
                is com.github.xepozz.maintainers.toolWindow.tree.GroupHeader -> userObject.title
                is com.github.xepozz.maintainers.toolWindow.tree.PackageManagerGroup -> userObject.packageManager.name
                is com.github.xepozz.maintainers.toolWindow.tree.DependencyGroup -> userObject.title
                is Dependency -> userObject.name
                is Maintainer -> userObject.name
                else -> userObject?.toString() ?: ""
            }
        }, false)
    }

    private fun showEmptyState() {
        closeActionToolbar.component.isVisible = false
        val stats = currentStats ?: return
        showMaintainerDetails()
        detailsPanel.showEmptyState(
            stats,
            onMaintainerClick = { maintainer ->
                selectMaintainer(maintainer)
            }
        )
    }

    private fun showMaintainerDetails() {
        detailsContainer.remove(packageDetailsPanel)
        detailsContainer.add(detailsPanel, BorderLayout.CENTER)
        detailsContainer.revalidate()
        detailsContainer.repaint()
    }

    private fun showPackageDetails() {
        detailsContainer.remove(detailsPanel)
        detailsContainer.add(packageDetailsPanel, BorderLayout.CENTER)
        detailsContainer.revalidate()
        detailsContainer.repaint()
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
            add(object : ToggleAction(
                MaintainersBundle.message("action.filter.funding.text"),
                MaintainersBundle.message("action.filter.funding.description"),
                AllIcons.Nodes.Favorite
            ) {
                override fun isSelected(e: AnActionEvent): Boolean {
                    return com.github.xepozz.maintainers.model.SearchFilter.parse(searchField.text).fundingOnly
                }

                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    filterController?.onFundingToggle(state)
                }
            })
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
            add(
                DefaultActionGroup(MaintainersBundle.message("action.view.options.text"), true)
                    .apply {
                        templatePresentation.icon = AllIcons.Actions.GroupBy
                        add(Separator(MaintainersBundle.message("action.group.by.text")))
                        add(object : ToggleAction(
                            MaintainersBundle.message("action.group.by.package.manager.text"),
                            MaintainersBundle.message("action.group.by.package.manager.description"),
                            null
                        ) {
                            override fun isSelected(e: AnActionEvent): Boolean = treeStructure.groupByPackageManager
                            override fun setSelected(e: AnActionEvent, state: Boolean) {
                                treeStructure.groupByPackageManager = state
                                structureModel.invalidateAsync()
                            }
                        })
                        add(object : ToggleAction(
                            MaintainersBundle.message("action.group.by.prefix.text"),
                            MaintainersBundle.message("action.group.by.prefix.description"),
                            null
                        ) {
                            override fun isSelected(e: AnActionEvent): Boolean = treeStructure.groupByPrefix
                            override fun setSelected(e: AnActionEvent, state: Boolean) {
                                treeStructure.groupByPrefix = state
                                structureModel.invalidateAsync()
                            }
                        })
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

        val toolbarPanel = JPanel(BorderLayout())
        toolbarPanel.isOpaque = false
        toolbarPanel.add(searchField, BorderLayout.CENTER)

        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        rightPanel.isOpaque = false
        managerPanel?.let {
            rightPanel.add(it)
        }
        rightPanel.add(actionToolbar.component)

        toolbarPanel.add(rightPanel, BorderLayout.EAST)
        toolbarPanel.border = JBUI.Borders.empty(2, 5)

        setToolbar(toolbarPanel)

        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = applyFilter()
            override fun removeUpdate(e: DocumentEvent?) = applyFilter()
            override fun changedUpdate(e: DocumentEvent?) = applyFilter()
        })
    }

    private fun refresh() {
        aggregatedData = MaintainerProvider.getAggregatedData(project)

        val activeManagers = aggregatedData.allDependencies.map { it.source }.distinct().sortedBy { it.name }
        if (managerPanel == null && activeManagers.isNotEmpty()) {
            val panel = PackageManagerFilterPanel(activeManagers) { manager, selected ->
                filterController?.onManagerToggle(manager, selected)
            }
            managerPanel = panel
            filterController = SearchFilterController(searchField, panel) { applyFilter() }
            setupToolbar() // Re-setup toolbar with new components
        }

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
        val filter = com.github.xepozz.maintainers.model.SearchFilter.parse(searchField.text)
        treeStructure.setFilter(filter)
        structureModel.invalidateAsync()

        val filteredMap = aggregatedData.maintainerMap.filter { (maintainer, dependencies) ->
            matchesFilter(maintainer, dependencies, filter)
        }

        val filteredDependencies = aggregatedData.allDependencies.filter { dependency ->
            matchesDependencyFilter(dependency, filter)
        }

        val maintainersCount = filteredMap.size
        val packagesCount = filteredDependencies.distinctBy { it.name }.size
        val fundingCount = filteredMap.keys.count { it.fundingLinks.isNotEmpty() }

        statusLabel.text = MaintainersBundle.message("toolwindow.status", maintainersCount, packagesCount, fundingCount)
    }

    private fun matchesFilter(
        maintainer: Maintainer,
        dependencies: List<Dependency>,
        filter: com.github.xepozz.maintainers.model.SearchFilter
    ): Boolean {
        if (filter.fundingOnly && maintainer.fundingLinks.isEmpty()) return false
        if (filter.packageManagers.isNotEmpty() && !maintainer.packages.any { it.packageManager in filter.packageManagers }) return false

        if (filter.textQuery.isEmpty()) return true

        return maintainer.name.lowercase().contains(filter.textQuery) ||
                dependencies.any { it.name.lowercase().contains(filter.textQuery) }
    }

    private fun matchesDependencyFilter(
        dependency: Dependency,
        filter: com.github.xepozz.maintainers.model.SearchFilter
    ): Boolean {
        if (filter.fundingOnly && !dependency.maintainers.any { it.fundingLinks.isNotEmpty() }) return false
        if (filter.packageManagers.isNotEmpty() && dependency.source !in filter.packageManagers) return false

        if (filter.textQuery.isEmpty()) return true

        return dependency.name.lowercase().contains(filter.textQuery) ||
                dependency.maintainers.any { it.name.lowercase().contains(filter.textQuery) }
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
