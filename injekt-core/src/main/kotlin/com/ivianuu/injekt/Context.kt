package com.ivianuu.injekt

interface Context {
    fun <T> getProviderOrNull(key: Key<T>): @Reader (() -> T)?
}

inline fun <reified T> Context.getOrNull(): T? =
    getOrNull(keyOf())

fun <T> Context.getOrNull(key: Key<T>): T? =
    getProviderOrNull(key)?.let { runReader { it() } }

inline fun <reified T> Context.get(): T = get(keyOf())

fun <T> Context.get(key: Key<T>): T = getProviderOrNull(key)?.let { runReader { it() } }
    ?: error("No given found for '$key'")

private class ContextImpl(
    private val parent: Context?,
    private val providers: Map<Key<*>, @Reader () -> Any?>
) : Context {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getProviderOrNull(key: Key<T>): @Reader (() -> T)? =
        providers[key] as? @Reader (() -> T)? ?: parent?.getProviderOrNull(key)
}

class ContextBuilder(private val parent: Context? = null) {
    private val providers = mutableMapOf<Key<*>, @Reader () -> Any?>()

    fun <T> unscoped(key: Key<T>, provider: @Reader () -> T) {
        providers[key] = provider
    }

    fun build(): Context = ContextImpl(parent, providers)
}

inline fun <reified T> ContextBuilder.unscoped(noinline provider: @Reader () -> T) {
    unscoped(keyOf(), provider)
}

inline fun rootContext(init: ContextBuilder.() -> Unit = {}): Context =
    ContextBuilder().apply(init).build()

inline fun Context.childContext(init: ContextBuilder.() -> Unit = {}): Context =
    ContextBuilder(this).apply(init).build()

@Reader
inline fun <reified T> given(): T = readerContext.get()

@Reader
val readerContext: Context
    get() = error("Intrinsic")
