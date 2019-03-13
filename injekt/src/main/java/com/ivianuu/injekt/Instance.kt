package com.ivianuu.injekt

/**
 * The [Instance] of an [Binding]
 */
abstract class Instance<T> {

    /**
     * The binding of this instance
     */
    abstract val binding: Binding<T>

    /**
     * Returns a instance of [T]
     */
    abstract fun get(
        component: Component,
        parameters: ParametersDefinition?
    ): T

    protected fun create(
        component: Component,
        parameters: ParametersDefinition?
    ): T {
        return try {
            binding.definition.invoke(
                DefinitionContext(component),
                parameters?.invoke() ?: emptyParameters()
            )
        } catch (e: Exception) {
            throw InstanceCreationException(
                "${component.componentName()} Couldn't instantiate $binding",
                e
            )
        }
    }

}