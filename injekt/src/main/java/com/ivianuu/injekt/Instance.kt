package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger

/**
 * The [Instance] of an [BeanDefinition]
 */
abstract class Instance<T : Any>(
    val definition: BeanDefinition<T>
) {

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
            definition.definition.invoke(
                DefinitionContext(component),
                parameters?.invoke() ?: emptyParameters()
            )
        } catch (e: Exception) {
            throw InstanceCreationException(
                "${component.name} Couldn't instantiate $definition",
                e
            )
        }
    }

}

/**
 * A [Instance] which creates a new value on every [get] call
 */
class FactoryInstance<T : Any>(
    definition: BeanDefinition<T>,
    val component: Component?
) : Instance<T>(definition) {

    override val isCreated: Boolean
        get() = false

    override fun get(
        component: Component,
        parameters: ParametersDefinition?
    ): T {
        val component = this.component ?: component
        logger?.info("${component.name} Create instance $definition")
        return create(component, parameters)
    }

}

/**
 * A [Instance] which creates the value 1 time per [Component] and caches the result
 */
class SingleInstance<T : Any>(
    definition: BeanDefinition<T>,
    val component: Component?
) : Instance<T>(definition) {

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
            logger?.info("${component.name} Return existing instance $definition")
            return value
        } else {
            logger?.info("${component.name} Create instance $definition")
            create(component, parameters).also { _value = it }
        }
    }

}