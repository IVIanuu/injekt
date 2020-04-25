package com.ivianuu.injekt

class Module @PublishedApi internal constructor(internal val bindings: Map<Key<*>, Binding<*>>)

class ModuleDsl {

    private val bindings = mutableMapOf<Key<*>, Binding<*>>()

    /**
     * Adds the [binding]
     */
    fun <T> add(binding: Binding<T>) {
        if (binding.duplicateStrategy.check(
                existsPredicate = { binding.key in bindings },
                errorMessage = { "Already declared binding for ${binding.key}" })
        ) {
            bindings[binding.key] = binding
        }
    }

    fun build(): Module = Module(bindings)

}

fun Module(block: ModuleDsl.() -> Unit = {}): Module =
    ModuleDsl().apply(block).build()
