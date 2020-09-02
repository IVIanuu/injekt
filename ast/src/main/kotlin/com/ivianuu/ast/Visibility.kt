

package com.ivianuu.ast

abstract class Visibility protected constructor(
    val name: String,
    val isPublicAPI: Boolean
) {
    open val internalDisplayName: String
        get() = name

    final override fun toString() = internalDisplayName

    open fun compareTo(visibility: Visibility): Int? {
        return Visibilities.compareLocal(this, visibility)
    }

    open fun normalize(): Visibility = this
}

