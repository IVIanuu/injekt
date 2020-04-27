package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider

internal class ProviderLazy<T>(private val provider: Provider<T>) : Lazy<T> {

    private var value: Any? = this

    override fun invoke(parameters: Parameters): T {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = provider(parameters)
                    this.value = value
                }
            }
        }

        return value as T
    }
}
