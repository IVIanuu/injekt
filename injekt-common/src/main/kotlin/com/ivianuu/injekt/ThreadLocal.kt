package com.ivianuu.injekt

/**
 * Holds instances in a thread local
 */
annotation class ThreadLocal {
    companion object : Behavior by (InterceptingBehavior {
        it.copy(provider = ThreadLocalProvider(it.provider))
    } + Bound)
}

inline fun <reified T> ComponentBuilder.threadLocal(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
) {
    threadLocal(
        key = keyOf(qualifier),
        behavior = behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

fun <T> ComponentBuilder.threadLocal(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    provider: BindingProvider<T>
) {
    bind(
        key = key,
        behavior = ThreadLocal + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

private class ThreadLocalProvider<T>(private val wrapped: BindingProvider<T>) :
        (Component, Parameters) -> T {

    private val threadLocal = object : java.lang.ThreadLocal<Any?>() {
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
