package com.ivianuu.injekt

/**
 * Holds instances in a thread local
 */
@GenerateDsl
@BehaviorMarker
val ThreadLocal = InterceptingBehavior {
    it.copy(provider = ThreadLocalProvider(it.provider))
} + Bound

private class ThreadLocalProvider<T>(private val wrapped: BindingProvider<T>) :
    AbstractBindingProvider<T>() {

    private val threadLocal = object : ThreadLocal<Any?>() {
        override fun initialValue() = this@ThreadLocalProvider
    }

    override fun doLink(linker: Linker) {
        wrapped.link(linker)
    }

    override fun invoke(parameters: Parameters): T {
        var value = threadLocal.get()
        if (value === this) {
            value = wrapped(parameters)
            threadLocal.set(value)
        }

        return value as T
    }

}
