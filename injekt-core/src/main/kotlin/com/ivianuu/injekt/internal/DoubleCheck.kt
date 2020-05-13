package com.ivianuu.injekt.internal

class DoubleCheck<T>(private var delegate: (() -> T)?) : () -> T {
    private var value: Any? = this
    override fun invoke(): T {
        var value = this.value
        if (value === this) {
            synchronized(this) {
                value = this.value
                if (value === this) {
                    value = delegate!!()
                    this.value = value
                    delegate = null
                }
            }
        }
        return value as T
    }
}
