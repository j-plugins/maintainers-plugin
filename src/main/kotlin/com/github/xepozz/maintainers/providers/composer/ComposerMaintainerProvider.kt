package com.github.xepozz.maintainers.providers.composer

import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.FundingSource
import com.github.xepozz.maintainers.model.Maintainer
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.InputStreamReader

class ComposerMaintainerProvider : MaintainerProvider {
    override fun getDependencies(project: Project): Collection<Dependency> {
        val lockFiles = FilenameIndex.getVirtualFilesByName("composer.lock", GlobalSearchScope.projectScope(project))
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

        val packages = root.getAsJsonArray("packages") ?: JsonArray()
        val packagesDev = root.getAsJsonArray("packages-dev") ?: JsonArray()

        val allPackages = mutableListOf<JsonObject>()
        packages.forEach { if (it is JsonObject) allPackages.add(it) }
        packagesDev.forEach { if (it is JsonObject) allPackages.add(it) }

        return allPackages.mapNotNull { pkg ->
            val name = pkg.get("name")?.asString ?: return@mapNotNull null
            val version = pkg.get("version")?.asString ?: ""
            val url = pkg.getAsJsonObject("source")?.get("url")?.asString
                ?: pkg.getAsJsonObject("support")?.get("source")?.asString

            val funding = pkg.getAsJsonArray("funding")?.mapNotNull { fundingElement ->
                if (fundingElement !is JsonObject) return@mapNotNull null
                val type = fundingElement.get("type")?.asString ?: return@mapNotNull null
                val url = fundingElement.get("url")?.asString ?: return@mapNotNull null
                FundingSource(type, url)
            } ?: emptyList()

            val maintainers = pkg.getAsJsonArray("authors")?.mapNotNull { authorElement ->
                if (authorElement !is JsonObject) return@mapNotNull null
                val authorName = authorElement.get("name")?.asString ?: return@mapNotNull null
                Maintainer(
                    name = authorName,
                    email = authorElement.get("email")?.asString,
                    homepage = authorElement.get("homepage")?.asString,
                    fundingLinks = funding
                )
            } ?: emptyList()

            Dependency(
                name = name,
                version = version,
                source = "composer",
                url = url,
                maintainers = maintainers
            )
        }
    }
}
