package com.ivianuu.injekt

import kotlin.collections.set
import kotlin.reflect.KClass

/**
 * The actual dependency container which provides bindings
 */
class Component @PublishedApi internal constructor() {

    private val dependencies = linkedSetOf<Component>()
    private val scopeNames = hashSetOf<String>()
    private val bindings = linkedMapOf<Key, Binding<*>>()
    private val instances = hashMapOf<Key, Instance<*>>()

    /**
     * Returns a instance of [T] matching the [type], [name] and [parameters]
     */
    fun <T> get(
        type: KClass<*>,
        name: String? = null,
        parameters: ParametersDefinition? = null
    ): T {
        val key = Key.of(type, name)

        val instance = findInstance<T>(key, true)
            ?: throw BindingNotFoundException("${componentName()} Could not find binding for $key")

        return instance.get(this, parameters)
    }

    /**
     * Adds all binding of the [module]
     */
    fun addModule(module: Module) {
        InjektPlugins.logger?.info("${componentName()} load module ${module.bindings.size}")
        module.bindings.forEach { addBinding(it.value) }
    }

    /**
     * Adds the [dependency] as a dependency
     */
    fun addDependency(dependency: Component) {
        synchronized(this) {
            if (!this.dependencies.add(dependency)) {
                error("Already added ${dependency.componentName()} to ${componentName()}")
            }
        }

        InjektPlugins.logger?.info("${componentName()} Add dependency $dependency")
    }

    /**
     * Returns all direct dependencies of this component
     */
    fun getDependencies(): Set<Component> = dependencies

    /**
     * Adds the [scopeName]
     */
    fun addScopeName(scopeName: String) {
        synchronized(this) {
            if (!this.scopeNames.add(scopeName)) {
                error("Scope name $scopeName was already added")
            }
        }
        InjektPlugins.logger?.info("${componentName()} Add scope name $scopeName")
    }

    /**
     * Returns all scope names of this component
     */
    fun getScopeNames(): Set<String> = scopeNames

    /**
     * Whether or not this component contains the [scopeName]
     */
    fun containsScopeName(scopeName: String): Boolean = scopeNames.contains(scopeName)

    /**
     * Returns all [Binding]s added to this component
     */
    fun getBindings(): Set<Binding<*>> = bindings.values.toSet()

    /**
     * Saves the [binding]
     */
    fun addBinding(binding: Binding<*>) {
        addBindingInternal(binding)
    }

    /**
     * Whether or not contains the [binding]
     */
    fun containsBinding(binding: Binding<*>): Boolean = bindings.containsKey(binding.key)

    /**
     * Returns all [Instance]s of this component
     */
    fun getInstances(): Set<Instance<*>> = instances.values.toSet()

    /**
     * Creates all eager instances of this component
     */
    fun createEagerInstances() {
        instances
            .filter { it.value.binding.eager && !it.value.isCreated }
            .forEach {
                InjektPlugins.logger?.info("${componentName()} Create eager instance for ${it.value.binding}")
                it.value.get(this, null)
            }
    }

    private fun <T> findInstance(key: Key, includeFactories: Boolean): Instance<T>? {
        return synchronized(this) {
            var instance = instances[key]

            if (instance != null) return@synchronized instance as Instance<T>

            for (dependency in dependencies) {
                instance = dependency.findInstance<T>(key, false)
                if (instance != null) return@synchronized instance
            }

            // we search for generated factories as a last resort
            if (includeFactories && key is Key.TypeKey) {
                try {
                    val factory = InjektPlugins.factoryFinder.find<T>(key.type)
                        ?: return@findInstance null
                    val binding = factory.create()
                    return@synchronized addBindingInternal(binding) as Instance<T>
                } catch (e: ClassNotFoundException) {
                    // ignore
                }
            }

            return@synchronized null
        }
    }

    private fun <T> createInstance(binding: Binding<T>): Instance<T> {
        val component = if (binding.scopeName != null) {
            findComponentForScope(binding.scopeName)
                ?: error("Cannot create instance for $binding unknown scope ${binding.scopeName}")
        } else {
            null
        }

        return binding.instanceFactory.create(binding, component)
    }

    private fun addBindingInternal(binding: Binding<*>): Instance<*> {
        return synchronized(this) {
            val isOverride = bindings.remove(binding.key) != null

            if (isOverride && !binding.override) {
                throw OverrideException("Try to override binding $binding but was already declared ${binding.key}")
            }

            bindings[binding.key] = binding

            if (binding.scopeName != null && !scopeNames.contains(binding.scopeName)) {
                val parentWithScope = findComponentForScope(binding.scopeName)

                // add the binding to the parent
                if (parentWithScope != null) {
                    return@addBindingInternal parentWithScope.addBindingInternal(binding)
                } else {
                    error("Component scope ${componentName()} does not match binding scope ${binding.scopeName}")
                }
            }

            val instance = createInstance(binding)

            instances[binding.key] = instance

            InjektPlugins.logger?.let { logger ->
                val msg = if (isOverride) {
                    "${componentName()} Override $binding"
                } else {
                    "${componentName()} Declare $binding"
                }
                logger.debug(msg)
            }

            return@synchronized instance
        }
    }

    private fun findComponentForScope(scopeName: String): Component? {
        if (scopeNames.contains(scopeName)) return this
        for (dependency in dependencies) {
            val result = dependency.findComponentForScope(scopeName)
            if (result != null) return result
        }

        return null
    }

}

/**
 * Defines a component
 */
typealias ComponentDefinition = Component.() -> Unit

/**
 * Returns a new [Component] and applies the [definition]
 */
inline fun component(
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition = {}
): Component {
    return Component()
        .apply {
            definition.invoke(this)
            if (createEagerInstances) {
                createEagerInstances()
            }
        }
}


/**
 * Adds all [modules]
 */
fun Component.modules(modules: Iterable<Module>) {
    modules.forEach(this::addModule)
}

/**
 * Adds all [modules]
 */
fun Component.modules(vararg modules: Module) {
    modules.forEach(this::addModule)
}

/**
 * Adds the module
 */
fun Component.modules(module: Module) {
    addModule(module)
}

/**
 * Adds all [dependencies]
 */
fun Component.dependencies(dependencies: Iterable<Component>) {
    dependencies.forEach(this::addDependency)
}

/**
 * Adds all [dependencies]
 */
fun Component.dependencies(vararg dependencies: Component) {
    dependencies.forEach(this::addDependency)
}

/**
 * Adds the [dependency]
 */
fun Component.dependencies(dependency: Component) {
    addDependency(dependency)
}

/**
 * Adds all of [scopeNames]
 */
fun Component.scopeNames(scopeNames: Iterable<String>) {
    scopeNames.forEach(this::addScopeName)
}

/**
 * Adds all [scopeNames]
 */
fun Component.scopeNames(vararg scopeNames: String) {
    scopeNames.forEach(this::addScopeName)
}

/**
 * Adds the [scopeName]
 */
fun Component.scopeNames(scopeName: String) {
    addScopeName(scopeName)
}

/**
 * Returns a instance of [T] matching the [name] and [parameters]
 */
inline fun <reified T> Component.get(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(T::class, name, parameters)

/**
 * Lazily returns a instance of [T] matching the [name] and [parameters]
 */
inline fun <reified T> Component.inject(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = lazy { get<T>(name, parameters) }

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T> Component.getProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = provider { parameters: ParametersDefinition? ->
    get<T>(name, parameters ?: defaultParameters)
}

/**
 * Returns a [Provider] for [T] and [name]
 * Each [Provider.get] call results in a potentially new value
 */
inline fun <reified T> Component.injectProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = lazy {
    provider { parameters: ParametersDefinition? ->
        get<T>(name, parameters ?: defaultParameters)
    }
}

fun Component.componentName(): String {
    return if (getScopeNames().isNotEmpty()) {
        "Component[${getScopeNames().joinToString(",")}]"
    } else {
        "Component[Unscoped]"
    }
}