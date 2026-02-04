package com.github.xepozz.maintainers.toolWindow.details

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import com.intellij.util.ImageLoader
import com.intellij.util.ui.JBImageIcon
import com.intellij.util.ui.ImageUtil
import java.awt.Image
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

object AvatarLoader {
    private val cache = ConcurrentHashMap<String, Icon>()

    fun getIconIfLoaded(githubUsername: String, size: Int): Icon? {
        return getIconByUrlIfLoaded("https://github.com/$githubUsername.png", size)
    }

    fun getIconByUrlIfLoaded(url: String, size: Int): Icon? {
        return cache["$url-$size"]
    }

    fun loadIcon(githubUsername: String, size: Int, callback: (Icon) -> Unit) {
        loadIconByUrl("https://github.com/$githubUsername.png", size, callback)
    }

    fun loadIconByUrl(url: String, size: Int, callback: (Icon) -> Unit) {
        val cacheKey = "$url-$size"
        cache[cacheKey]?.let {
            callback(it)
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val image = ImageLoader.loadFromUrl(URL(url))
                if (image != null) {
                    val icon = createSquareIcon(image, size)
                    cache[cacheKey] = icon
                    ApplicationManager.getApplication().invokeLater {
                        callback(icon)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun createSquareIcon(image: Image, size: Int): Icon {
        val buffered = ImageUtil.toBufferedImage(image)

        // Crop to square from center
        val minSide = minOf(buffered.width, buffered.height)
        val x = (buffered.width - minSide) / 2
        val y = (buffered.height - minSide) / 2
        val cropped = buffered.getSubimage(x, y, minSide, minSide)

        // Scale to target size
        val scaled = cropped.getScaledInstance(size, size, Image.SCALE_SMOOTH)

        return JBImageIcon(ImageUtil.toBufferedImage(scaled))
    }
}
