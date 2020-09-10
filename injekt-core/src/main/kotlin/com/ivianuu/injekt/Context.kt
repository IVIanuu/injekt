package com.ivianuu.injekt

interface Context {
    fun <T> givenProviderOrNull(key: Key<T>): @Reader (() -> T)?
}

inline fun <reified T> Context.givenOrNull(): T? =
    givenOrNull(keyOf())

fun <T> Context.givenOrNull(key: Key<T>): T? =
    givenProviderOrNull(key)?.let { runReader { it() } }

inline fun <reified T> Context.given(): T = given(keyOf())

fun <T> Context.given(key: Key<T>): T = givenProviderOrNull(key)?.let { runReader { it() } }
    ?: error("No given found for '$key'")

private class ContextImpl(
    private val parent: Context?,
    private val providers: Map<Key<*>, @Reader () -> Any?>
) : Context {
    @Suppress("UNCHECKED_CAST")
    override fun <T> givenProviderOrNull(key: Key<T>): @Reader (() -> T)? =
        providers[key] as? @Reader (() -> T)? ?: parent?.givenProviderOrNull(key)
}

class ContextBuilder(private val parent: Context? = null) {
    private val providers = mutableMapOf<Key<*>, @Reader () -> Any?>()

    fun <T> given(key: Key<T>, provider: @Reader () -> T) {
        providers[key] = provider
    }

    fun build(): Context = ContextImpl(parent, providers)
}

inline fun <reified T> ContextBuilder.given(noinline provider: @Reader () -> T) {
    given(keyOf(), provider)
}

inline fun rootContext(init: ContextBuilder.() -> Unit = {}): Context =
    ContextBuilder().apply(init).build()

inline fun Context.childContext(init: ContextBuilder.() -> Unit = {}): Context =
    ContextBuilder(this).apply(init).build()

@JvmName("readerGiven")
@Reader
inline fun <reified T> given(): T = currentContext.given()

@Reader
val currentContext: Context
    get() = error("Intrinsic")
