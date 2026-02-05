package com.github.xepozz.maintainers.toolWindow.tree

data class GroupHeader(val id: String, val title: String, val count: Int = 0) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupHeader) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
