package com.github.xepozz.maintainers.services

import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.Maintainer
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MaintainersService(private val project: Project) {

    fun getAggregatedMaintainers(): Map<Maintainer, Int> {
        return MaintainerProvider.getAggregatedMaintainers(project)
    }
}
