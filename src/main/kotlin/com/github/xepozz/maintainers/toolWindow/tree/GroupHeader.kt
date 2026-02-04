package com.github.xepozz.maintainers.toolWindow.tree

data class GroupHeader(val title: String, val count: Int = 0) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupHeader) return false
        return title == other.title
    }

    override fun hashCode(): Int {
        return title.hashCode()
    }
}
