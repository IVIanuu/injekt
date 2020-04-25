package com.ivianuu.injekt

import kotlin.reflect.KClass

inline fun <reified T> ModuleDsl.instance(
    instance: T,
    qualifier: KClass<*>? = null,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) {
    instance(
        instance = instance,
        key = keyOf(qualifier),
        duplicateStrategy = duplicateStrategy
    )
}

/**
 * Adds the [instance] as a binding for [key]
 */
fun <T> ModuleDsl.instance(
    instance: T,
    key: Key<T>,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) {
    add(
        Binding(
            key = key,
            duplicateStrategy = duplicateStrategy,
            InstanceProvider(instance)
        )
    )
}

internal class InstanceProvider<T>(private val instance: T) : Provider<T> {
    override fun invoke(parameters: Parameters) = instance
}
