package com.ivianuu.injekt

/**
 * Provides instances lazily and caches results
 */
interface Lazy<T> {
    operator fun invoke(parameters: ParametersDefinition? = null): T
}

internal class ProviderLazy<T>(private val provider: Provider<T>) : Lazy<T> {
    private var _value: Any? = this

    override fun invoke(parameters: ParametersDefinition?): T {
        var value = _value
        if (value === this) {
            synchronized(this) {
                value = _value
                if (value === this) {
                    _value = provider(parameters)
                    value = _value
                }
            }
        }

        return value as T
    }
}

internal class KeyedLazy<T>(
    private val component: Component,
    private val key: Key
) : Lazy<T> {

    private var _value: Any? = this

    override fun invoke(parameters: ParametersDefinition?): T {
        var value = _value
        if (value === this) {
            synchronized(this) {
                value = _value
                if (value === this) {
                    _value = component.get<T>(key, parameters)
                    value = _value
                }
            }
        }

        return value as T
    }

}