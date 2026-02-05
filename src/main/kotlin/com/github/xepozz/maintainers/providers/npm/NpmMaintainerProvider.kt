package com.github.xepozz.maintainers.providers.npm

import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.FundingSource
import com.github.xepozz.maintainers.model.Maintainer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.InputStreamReader

class NpmMaintainerProvider : MaintainerProvider {
    override fun getDependencies(project: Project): Collection<Dependency> {
        val lockFiles = FilenameIndex.getVirtualFilesByName("package-lock.json", GlobalSearchScope.projectScope(project))
        return lockFiles.flatMap { parseLockFile(it) }
    }

    private fun parseLockFile(file: VirtualFile): Collection<Dependency> {
        val root = try {
            file.inputStream.use { stream ->
                InputStreamReader(stream).use { reader ->
                    JsonParser.parseReader(reader).asJsonObject
                }
            }
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        val packages = root.getAsJsonObject("packages") ?: return emptyList()

        return packages.entrySet().mapNotNull { (key, element) ->
            if (key.isEmpty() || element !is JsonObject) return@mapNotNull null
            
            // Key is usually "node_modules/name" or "packages/name" (for workspaces)
            val name = key.substringAfterLast("node_modules/")
                .substringAfterLast("packages/")

            val version = element.get("version")?.asString ?: ""
            val resolved = element.get("resolved")?.asString

            val maintainers = emptyList<Maintainer>()

            Dependency(
                name = name,
                version = version,
                source = NpmPackageManager,
                url = resolved,
                maintainers = maintainers
            )
        }
    }

    private fun extractIconFromFunding(sources: List<FundingSource>): String? {
        val githubSource = sources.find { it.url.contains("github.com/") }
        if (githubSource != null) {
            val url = githubSource.url
            val username = url.substringAfter("github.com/")
                .substringAfter("sponsors/")
                .substringBefore("/")
                .substringBefore("?")
            if (username.isNotEmpty()) {
                return "https://github.com/$username.png"
            }
        }
        return null
    }

    private fun extractFunding(fundingElement: JsonElement?): List<FundingSource> {
        if (fundingElement == null) return emptyList()
        
        val sources = mutableListOf<FundingSource>()
        
        if (fundingElement.isJsonObject) {
            val obj = fundingElement.asJsonObject
            val type = obj.get("type")?.asString ?: "unknown"
            val url = obj.get("url")?.asString
            if (url != null) {
                sources.add(FundingSource(type, url))
            }
        } else if (fundingElement.isJsonArray) {
            fundingElement.asJsonArray.forEach {
                sources.addAll(extractFunding(it))
            }
        } else if (fundingElement.isJsonPrimitive && fundingElement.asJsonPrimitive.isString) {
            sources.add(FundingSource("individual", fundingElement.asString))
        }
        
        return sources
    }
}
