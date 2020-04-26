package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Parameters

internal class KeyedLazy<T>(
    private val component: Component,
    private val key: Key<T>
) : Lazy<T> {

    private var value: Any? = this

    override fun invoke(parameters: Parameters): T {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = component.get(key, parameters)
                    this.value = value
                }
            }
        }

        return value as T
    }
}
