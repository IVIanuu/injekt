package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Provider

class ScopedProvider<T>(private var wrapped: Provider<T>?) : Provider<T> {
    private var value: Any? = this

    override fun invoke(): T {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = wrapped!!()
                    this.value = value
                    wrapped = null
                }
            }
        }

        return value as T
    }
}
