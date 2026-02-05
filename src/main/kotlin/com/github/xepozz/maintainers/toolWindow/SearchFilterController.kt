package com.github.xepozz.maintainers.toolWindow

import com.github.xepozz.maintainers.model.PackageManager
import com.github.xepozz.maintainers.model.SearchFilter
import com.intellij.ui.SearchTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SearchFilterController(
    private val searchField: SearchTextField,
    private val managerPanel: PackageManagerFilterPanel,
    private val onFilterUpdate: () -> Unit
) {
    private var isUpdating = false

    init {
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateFromText()
            override fun removeUpdate(e: DocumentEvent?) = updateFromText()
            override fun changedUpdate(e: DocumentEvent?) = updateFromText()
        })
    }

    private fun updateFromText() {
        if (isUpdating) return
        val filter = SearchFilter.parse(searchField.text)
        managerPanel.updateState(filter.packageManagers)
        onFilterUpdate()
    }

    fun onManagerToggle(manager: PackageManager, selected: Boolean) {
        isUpdating = true
        try {
            val filter = SearchFilter.parse(searchField.text)
            val newManagers = if (selected) {
                filter.packageManagers + manager
            } else {
                filter.packageManagers - manager
            }
            val newFilter = filter.copy(packageManagers = newManagers)
            searchField.text = newFilter.toText()
            // updateFromText() will be called by document listener
        } finally {
            isUpdating = false
        }
    }
}
