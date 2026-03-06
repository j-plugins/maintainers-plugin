package com.github.xepozz.maintainers.providers.ide

import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.project.Project

class IdeMaintainerProvider : MaintainerProvider {
    override val packageManager = IdePackageManager

    override fun getDependencies(project: Project): Collection<Dependency> {
        return PluginManager.getPlugins()
//            .filter { !it.isBundled && it.isEnabled }
            .map { descriptor ->
                Dependency(
                    name = descriptor.name,
                    version = descriptor.version ?: "unknown",
                    source = packageManager,
                    url = descriptor.url ?: "https://plugins.jetbrains.com/plugin/index?xmlId=${descriptor.pluginId.idString}",
                    maintainers = buildMaintainer(descriptor)
                )
            }
    }

    private fun buildMaintainer(descriptor: IdeaPluginDescriptor): List<Maintainer> {
        val vendor = descriptor.vendor
        if (vendor.isNullOrBlank()) return emptyList()

        return listOf(
            Maintainer(
                name = vendor,
                email = descriptor.vendorEmail,
                homepage = descriptor.vendorUrl,
                github = extractGithubUsername(descriptor.vendorUrl),
                icon = extractGithubUsername(descriptor.vendorUrl)?.let { "https://github.com/$it.png" },
                fundingLinks = emptyList()
            )
        )
    }

    private fun extractGithubUsername(url: String?): String? {
        if (url == null) return null
        val prefix = "github.com/"
        val index = url.indexOf(prefix)
        if (index == -1) return null
        val path = url.substring(index + prefix.length).removeSuffix("/")
        return path.split("/").firstOrNull()?.takeIf { it.isNotEmpty() }
    }
}
