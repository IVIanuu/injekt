package com.ivianuu.injekt

/**
 * The [Instance] of an [Binding]
 */
interface Instance<T> {

    /**
     * The binding of this instance
     */
    val binding: Binding<T>

    /**
     * Whether or not this instance is created
     */
    val isCreated: Boolean

    /**
     * Returns a instance of [T]
     */
    fun get(
        component: Component,
        parameters: ParametersDefinition?
    ): T

    fun create(
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