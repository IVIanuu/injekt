package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger

/**
 * The [Instance] of an [BeanDefinition]
 */
abstract class Instance<T : Any>(val beanDefinition: BeanDefinition<T>) {

    /**
     * The component this instance lives in
     */
    lateinit var component: Component

    /**
     * Whether or not this instance is created
     */
    abstract val isCreated: Boolean

    /**
     * Returns a instance of [T]
     */
    abstract fun get(params: ParamsDefinition? = null): T

    protected open fun create(params: ParamsDefinition?): T {
        if (beanDefinition.scopeId != null && beanDefinition.scopeId != component.scopeId) {
            error("Component scope ${component.name} does not match definition scope ${beanDefinition.scopeId}")
        }

        return try {
            beanDefinition.definition.invoke(
                DefinitionContext(component),
                params?.invoke() ?: emptyParameters()
            )
        } catch (e: Exception) {
            throw InstanceCreationException(
                "${component.name} Couldn't instantiate $beanDefinition",
                e
            )
        }
    }

}

/**
 * A [Instance] which creates a new value on every [get] call
 */
class FactoryInstance<T : Any>(
    beanDefinition: BeanDefinition<T>
) : Instance<T>(beanDefinition) {

    override val isCreated: Boolean
        get() = false

    override fun get(params: ParamsDefinition?): T {
        logger?.info("${component.name} Create instance $beanDefinition")
        return create(params)
    }

}

/**
 * A [Instance] which creates the value 1 time per [Component] and caches the result
 */
class SingleInstance<T : Any>(
    beanDefinition: BeanDefinition<T>
) : Instance<T>(beanDefinition) {

    private var _value: T? = null

    override val isCreated: Boolean
        get() = _value != null

    override fun get(params: ParamsDefinition?): T {
        val value = _value

        return if (value != null) {
            logger?.info("${component.name} Return existing instance $beanDefinition")
            return value
        } else {
            logger?.info("${component.name} Create instance $beanDefinition")
            create(params).also { _value = it }
        }
    }

}