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

/**
 * A [Instance] which creates a new value on every [get] call
 */
class FactoryInstance<T>(
    override val binding: Binding<T>,
    val component: Component?
) : Instance<T> {

    override val isCreated: Boolean
        get() = false

    override fun get(
        component: Component,
        parameters: ParametersDefinition?
    ): T {
        val component = this.component ?: component
        InjektPlugins.logger?.info("${component.componentName()} Create instance $binding")
        return create(component, parameters)
    }

}

/**
 * Factory for [FactoryInstance]s
 */
object FactoryInstanceFactory : InstanceFactory {
    override fun <T> create(binding: Binding<T>, component: Component?): Instance<T> =
        FactoryInstance(binding, component)
}

/**
 * A [Instance] which creates the value 1 time per [Component] and caches the result
 */
class SingleInstance<T>(
    override val binding: Binding<T>,
    val component: Component?
) : Instance<T> {

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
            InjektPlugins.logger?.info("${component.componentName()} Return existing instance $binding")
            return value
        } else {
            InjektPlugins.logger?.info("${component.componentName()} Create instance $binding")
            create(component, parameters).also { _value = it }
        }
    }

}

/**
 * Factory for [SingleInstance]s
 */
object SingleInstanceFactory : InstanceFactory {
    override fun <T> create(binding: Binding<T>, component: Component?): Instance<T> =
        SingleInstance(binding, component)
}