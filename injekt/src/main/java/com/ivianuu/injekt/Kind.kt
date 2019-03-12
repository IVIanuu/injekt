package com.ivianuu.injekt

private const val UNKNOWN_KIND = "Unknown"

/**
 * Instance kind for
 */
interface Kind {
    fun <T> createInstance(binding: Binding<T>, component: Component?): Instance<T>
    fun asString(): String = UNKNOWN_KIND
}