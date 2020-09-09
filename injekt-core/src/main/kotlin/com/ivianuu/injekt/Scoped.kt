package com.ivianuu.injekt

inline fun <reified T> ContextBuilder.scoped(noinline provider: @Reader () -> T) {
    scoped(keyOf(), provider)
}

fun <T> ContextBuilder.scoped(
    key: Key<T>,
    provider: @Reader () -> T
) {
    unscoped(key, ScopedProvider(provider))
}

private class ScopedProvider<T>(
    provider: @Reader () -> T
) : @Reader () -> T {
    var _provider: @Reader (() -> T)? = provider
    private var _value: Any? = this

    @Reader
    override fun invoke(): T {
        var value: Any? = _value
        if (value === this) {
            synchronized(this) {
                value = _value
                if (value === this) {
                    value = _provider!!()
                    _value = value
                    _provider = null
                }
            }
        }
        return value as T
    }
}
