package com.ivianuu.injekt

private const val UNKNOWN_KIND = "Unknown"

/**
 * Instance kind for
 */
interface Kind {
    /**
     * Creates a [Instance] for this kind
     */
    fun <T> createInstance(binding: Binding<T>, component: Component?): Instance<T>

    /**
     * String representation for this kind
     */
    fun asString(): String = UNKNOWN_KIND
}