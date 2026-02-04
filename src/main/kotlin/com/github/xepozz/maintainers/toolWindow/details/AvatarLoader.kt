package com.github.xepozz.maintainers.toolWindow.details

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.ImageUtil
import java.awt.Image
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

object AvatarLoader {
    private val iconCache = ContainerUtil.createConcurrentSoftValueMap<String, Icon>()
    private val loadingRequests = ConcurrentHashMap<String, CompletableFuture<Icon?>>()

    fun getIconIfLoaded(githubUsername: String, size: Int): Icon? {
        return getIconByUrlIfLoaded(getGithubUrl(githubUsername, size), size)
    }

    fun getIconByUrlIfLoaded(url: String, size: Int): Icon? {
        return iconCache["$url-$size"]
    }

    fun loadIcon(githubUsername: String, size: Int, callback: (Icon) -> Unit) {
        loadIconByUrl(getGithubUrl(githubUsername, size), size, callback)
    }

    private fun getGithubUrl(username: String, size: Int): String {
        // Request double size for better HiDPI support
        return "https://github.com/$username.png?s=${size * 2}"
    }

    fun loadIconByUrl(url: String, size: Int, callback: (Icon) -> Unit) {
        val cacheKey = "$url-$size"
        val cached = iconCache[cacheKey]
        if (cached != null) {
            callback(cached)
            return
        }

        loadingRequests.computeIfAbsent(cacheKey) { _ ->
            val future = CompletableFuture<Icon?>()
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    // Use platform's IconLoader to leverage its internal caching
                    val icon = IconLoader.findIcon(URL(url), true)
                    if (icon != null) {
                        val processed = processIcon(icon, size)
                        future.complete(processed)
                    } else {
                        future.complete(null)
                    }
                } catch (e: Exception) {
                    future.complete(null)
                } finally {
                    loadingRequests.remove(cacheKey)
                }
            }
            future
        }.thenAccept { icon ->
            if (icon != null) {
                iconCache[cacheKey] = icon
                ApplicationManager.getApplication().invokeLater {
                    callback(icon)
                }
            }
        }
    }

    private fun processIcon(icon: Icon, size: Int): Icon {
        // Trigger load if it's a lazy icon
        val width = icon.iconWidth
        val height = icon.iconHeight

        if (width <= 0 || height <= 0) return icon

        val image = IconUtil.toImage(icon)
        val buffered = ImageUtil.toBufferedImage(image)

        // Crop to square from center
        val minSide = minOf(buffered.width, buffered.height)
        val x = (buffered.width - minSide) / 2
        val y = (buffered.height - minSide) / 2
        val cropped = buffered.getSubimage(x, y, minSide, minSide)

        // Scale to target size
        val scaled = cropped.getScaledInstance(size, size, Image.SCALE_SMOOTH)

        return IconUtil.createImageIcon(ImageUtil.toBufferedImage(scaled))
    }
}
