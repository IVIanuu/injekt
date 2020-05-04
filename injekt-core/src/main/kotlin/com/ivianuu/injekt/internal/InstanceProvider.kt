package com.ivianuu.injekt.internal

class InstanceProvider<T>(private val instance: T) : () -> T {

    override fun invoke() = instance

    companion object {
        private val NullProvider = InstanceProvider<Any?>(null)

        fun <T> create(instance: T): InstanceProvider<T> {
            return if (instance == null) NullProvider as InstanceProvider<T>
            else InstanceProvider(instance)
        }
    }
}
