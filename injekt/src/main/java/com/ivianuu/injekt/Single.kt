package com.ivianuu.injekt

/**
 * Factory kind
 */
object SingleKind : Kind {
    private const val SINGLE_KIND = "Single"

    override fun <T> createInstance(binding: Binding<T>, component: Component?): Instance<T> =
        SingleInstance(binding, component)

    override fun asString(): String = SINGLE_KIND
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
 * Provides scoped dependency which will be created once for each component
 */
inline fun <reified T> Module.single(
    qualifier: Qualifier? = null,
    scopeName: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = add(
    Binding.create(
        type = T::class,
        qualifier = qualifier,
        kind = SingleKind,
        scopeName = scopeName,
        override = override,
        eager = eager,
        definition = definition
    )
)