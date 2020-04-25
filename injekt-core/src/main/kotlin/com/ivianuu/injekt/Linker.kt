package com.ivianuu.injekt

import kotlin.reflect.KClass

class Linker(val component: Component) {

    private val linkedKeys = mutableSetOf<Key<*>>()

    inline fun <reified T> get(qualifier: KClass<*>? = null): Provider<T> =
        get(keyOf(qualifier))

    fun <T> get(key: Key<T>): Provider<T> {
        // todo
        if (key is Key.ParameterizedKey && key.arguments.size == 1) {
            fun instanceKey() = keyOf<T>(
                classifier = key.arguments.single().classifier,
                qualifier = key.qualifier
            )
            when (key.arguments.single().classifier) {
                Lazy::class -> InstanceProvider(KeyedLazy(component, instanceKey()))
                Provider::class -> InstanceProvider(KeyedProvider(this, instanceKey()))
            }
        }

        find(key)?.let { return it }

        if (key.isNullable) return NullProvider as Provider<T>

        error("Couldn't find binding for $key")
    }

    private fun <T> find(key: Key<T>): Provider<T>? {
        component.bindings[key]
            ?.provider
            ?.also { if (linkedKeys.add(key)) (it as? Linkable)?.link(this) }
            ?.let { return it as? Provider<T> }

        return component.parent?.linker?.find(key)
    }

    private object NullProvider : Provider<Nothing?> {
        override fun invoke(parameters: Parameters) = null
    }
}

interface Linkable {
    fun link(linker: Linker)
}
