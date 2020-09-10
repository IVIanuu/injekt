package com.ivianuu.injekt

interface Context {
    fun <T> givenProviderOrNull(key: Key<T>): @Reader (() -> T)?
}

internal class ContextImpl(
    private val parent: Context?,
    private val providers: Map<Key<*>, @Reader () -> Any?>
) : Context {
    @Suppress("UNCHECKED_CAST")
    override fun <T> givenProviderOrNull(key: Key<T>): @Reader (() -> T)? =
        providers[key] as? @Reader (() -> T)? ?: parent?.givenProviderOrNull(key)
}

@Reader
val currentContext: Context
    get() = error("Intrinsic")
