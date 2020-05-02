package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Lazy

class InstanceProvider<T>(private val instance: T) : Lazy<T> {
    override fun invoke() = instance

    companion object {
        private val NullProvider = InstanceProvider<Any?>(null)

        fun <T> create(instance: T): InstanceProvider<T> {
            return if (instance == null) NullProvider as InstanceProvider<T>
            else InstanceProvider(instance)
        }
    }
}
