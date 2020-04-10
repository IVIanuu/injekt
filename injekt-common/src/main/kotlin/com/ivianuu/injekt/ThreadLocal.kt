package com.ivianuu.injekt

/**
 * Holds instances in a thread local
 */
@GenerateDslBuilder
@BehaviorMarker
val ThreadLocal = InterceptingBehavior {
    it.copy(provider = ThreadLocalProvider(it.provider))
} + Bound

private class ThreadLocalProvider<T>(private val wrapped: BindingProvider<T>) :
        (Component, Parameters) -> T {

    private val threadLocal = object : ThreadLocal<Any?>() {
        override fun initialValue() = this@ThreadLocalProvider
    }

    override fun invoke(component: Component, parameters: Parameters): T {
        var value = threadLocal.get()
        if (value === this) {
            value = wrapped(component, parameters)
            threadLocal.set(value)
        }

        return value as T
    }

}
