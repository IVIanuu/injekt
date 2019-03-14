package com.ivianuu.injekt

/**
 * Factory kind
 */
object FactoryKind : Kind {
    private const val FACTORY_KIND = "Factory"

    override fun <T> createInstance(binding: Binding<T>, component: Component?): Instance<T> =
        FactoryInstance(binding, component)

    override fun asString(): String = FACTORY_KIND

}

/**
 * A [Instance] which creates a new value on every [get] call
 */
class FactoryInstance<T>(
    override val binding: Binding<T>,
    val component: Component?
) : Instance<T>() {

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
 * Provides a unscoped dependency which will be recreated on each request
 */
inline fun <reified T> Module.factory(
    qualifier: Qualifier? = null,
    scope: Scope? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = add(
    Binding(
        type = T::class,
        qualifier = qualifier,
        kind = FactoryKind,
        scope = scope,
        override = override,
        definition = definition
    )
)