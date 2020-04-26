package com.ivianuu.injekt

class Module @PublishedApi internal constructor(
    internal val bindings: Map<Key<*>, Binding<*>>
)

class ModuleDsl {

    private val bindings = mutableMapOf<Key<*>, Binding<*>>()

    /**
     * Registers the [binding] for [key]
     */
    fun <T> add(
        key: Key<T>,
        binding: Binding<T>
    ) {
        if (key in bindings) {
            error("Already declared binding for $key")
        }

        bindings[key] = binding
    }

    fun build(): Module = Module(bindings)

}

fun Module(block: ModuleDsl.() -> Unit = {}): Module =
    ModuleDsl().apply(block).build()
