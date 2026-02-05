package com.github.xepozz.maintainers.providers.go

import com.github.xepozz.maintainers.extension.MaintainerProvider
import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

class GoMaintainerProvider : MaintainerProvider {
    override val packageManager = GoPackageManager

    override fun getDependencies(project: Project): Collection<Dependency> {
        val goSumFiles = FilenameIndex.getVirtualFilesByName("go.sum", GlobalSearchScope.projectScope(project))
        return goSumFiles.flatMap { parseGoSum(it) }
    }

    private fun parseGoSum(file: VirtualFile): List<Dependency> {
        val content = try {
            file.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            return emptyList()
        }

        val dependencies = mutableMapOf<String, Dependency>()

        content.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            // Format: module/path version hash
            // Example: github.com/google/uuid v1.6.0 h1:NIva...
            val parts = trimmed.split("\\s+".toRegex())
            if (parts.size < 2) return@forEach

            val modulePath = parts[0]
            val versionPart = parts[1]
            
            // Skip go.mod checksum entries
            if (versionPart.endsWith("/go.mod")) return@forEach

            val key = "$modulePath@$versionPart"
            
            if (!dependencies.containsKey(key)) {
                dependencies[key] = Dependency(
                    name = modulePath,
                    version = versionPart,
                    source = packageManager,
                    url = "https://pkg.go.dev/$modulePath@$versionPart",
                    maintainers = extractMaintainersFromPath(modulePath)
                )
            }
        }

        return dependencies.values.toList()
    }

    private fun extractMaintainersFromPath(modulePath: String): List<Maintainer> {
        return when {
            modulePath.startsWith("github.com/") -> {
                val org = modulePath.removePrefix("github.com/").substringBefore("/")
                listOf(
                    Maintainer(
                        name = org,
                        email = null,
                        homepage = "https://github.com/$org",
                        github = org,
                        icon = "https://github.com/$org.png",
                        fundingLinks = emptyList()
                    )
                )
            }
            modulePath.startsWith("gitlab.com/") -> {
                val org = modulePath.removePrefix("gitlab.com/").substringBefore("/")
                listOf(
                    Maintainer(
                        name = org,
                        email = null,
                        homepage = "https://gitlab.com/$org",
                        github = null,
                        icon = null,
                        fundingLinks = emptyList()
                    )
                )
            }
            modulePath.startsWith("golang.org/x/") -> listOf(
                Maintainer(
                    name = "Go Team",
                    email = null,
                    homepage = "https://go.dev",
                    github = "golang",
                    icon = "https://github.com/golang.png",
                    fundingLinks = emptyList()
                )
            )
            modulePath.startsWith("go.uber.org/") -> listOf(
                Maintainer(
                    name = "Uber",
                    email = null,
                    homepage = "https://github.com/uber-go",
                    github = "uber-go",
                    icon = "https://github.com/uber-go.png",
                    fundingLinks = emptyList()
                )
            )
            modulePath.startsWith("google.golang.org/") -> listOf(
                Maintainer(
                    name = "Google",
                    email = null,
                    homepage = "https://github.com/googleapis",
                    github = "googleapis",
                    icon = "https://github.com/google.png",
                    fundingLinks = emptyList()
                )
            )
            modulePath.startsWith("go.opentelemetry.io/") -> listOf(
                Maintainer(
                    name = "OpenTelemetry",
                    email = null,
                    homepage = "https://opentelemetry.io",
                    github = "open-telemetry",
                    icon = "https://github.com/open-telemetry.png",
                    fundingLinks = emptyList()
                )
            )
            modulePath.startsWith("go.temporal.io/") -> listOf(
                Maintainer(
                    name = "Temporal",
                    email = null,
                    homepage = "https://temporal.io",
                    github = "temporalio",
                    icon = "https://github.com/temporalio.png",
                    fundingLinks = emptyList()
                )
            )
            else -> emptyList()
        }
    }
}