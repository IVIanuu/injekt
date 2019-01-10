package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger

/**
 * The [Instance] of an [Binding]
 */
abstract class Instance<T : Any>(val binding: Binding<T>) {

    /**
     * Whether or not this instance is created
     */
    abstract val isCreated: Boolean

    /**
     * Returns a instance of [T]
     */
    abstract fun get(
        component: Component,
        parameters: ParametersDefinition?
    ): T

    protected open fun create(
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
                "${component.name} Couldn't instantiate $binding",
                e
            )
        }
    }

}

/**
 * A [Instance] which creates a new value on every [get] call
 */
class FactoryInstance<T : Any>(
    binding: Binding<T>,
    val component: Component?
) : Instance<T>(binding) {

    override val isCreated: Boolean
        get() = false

    override fun get(
        component: Component,
        parameters: ParametersDefinition?
    ): T {
        val component = this.component ?: component
        logger?.info("${component.name} Create instance $binding")
        return create(component, parameters)
    }

}

/**
 * A [Instance] which creates the value 1 time per [Component] and caches the result
 */
class SingleInstance<T : Any>(
    binding: Binding<T>,
    val component: Component?
) : Instance<T>(binding) {

    private var _value: T? = null

    override val isCreated: Boolean
        get() = _value != null

    override fun get(
        component: Component,
        parameters: ParametersDefinition?
    ): T {
        val component = this.component ?: component
        val value = _value

        return if (value != null) {
            logger?.info("${component.name} Return existing instance $binding")
            return value
        } else {
            logger?.info("${component.name} Create instance $binding")
            create(component, parameters).also { _value = it }
        }
    }

}