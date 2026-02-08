package com.github.xepozz.maintainers.services

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "MaintainersSettings", storages = [Storage("maintainers_settings.xml")])
class MaintainersSettingsService : BaseState(), PersistentStateComponent<MaintainersSettingsService> {

    var groupByPackageManager by property(true)
    var groupByPrefix by property(true)

    override fun getState(): MaintainersSettingsService = this

    override fun loadState(state: MaintainersSettingsService) {
        copyFrom(state)
    }

    companion object {
        fun getInstance(project: Project): MaintainersSettingsService = project.service()
    }
}
