package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Lazy

class KeyedLazy<T>(
    private val key: Int,
    private val component: Component
) : Lazy<T> {

    private var value: Any? = this

    override fun invoke(): T {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = component.get(key)
                    this.value = value
                }
            }
        }

        return value as T
    }
}
