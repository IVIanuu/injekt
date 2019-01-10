package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * The actual dependency container which provides bindings
 */
class Component internal constructor(val name: String?) {

    private val dependencies = hashSetOf<Component>()
    private val scopeNames = hashSetOf<String>()
    private val bindings = hashMapOf<Key, Binding<*>>()
    private val instances = hashMapOf<Key, Instance<*>>()

    /**
     * Returns a instance of [T] matching the [type], [name] and [parameters]
     */
    fun <T : Any> get(
        type: KClass<T>,
        name: String? = null,
        parameters: ParametersDefinition? = null
    ): T {
        val key = Key(type, name)
        return findInstance<T>(key, true)?.get(this, parameters)
            ?: throw NoBeanDefinitionFoundException("${this.name} Could not find binding for $key")
    }

    /**
     * Adds all [Binding]s of the [modules]
     */
    fun modules(modules: Iterable<Module>) {
        modules.forEach { module ->
            InjektPlugins.logger?.info("$name load module ${module.name}")
            module.bindings.forEach { addBinding(it.value) }
        }
    }

    /**
     * Adds all of [dependencies] as dependencies
     */
    fun dependencies(dependencies: Iterable<Component>) {
        dependencies.forEach { addDependency(it) }
    }

    /**
     * Returns all direct dependencies of this component
     */
    fun getDependencies(): Set<Component> = dependencies

    /**
     * Adds the [dependency] as a dependency
     */
    fun addDependency(dependency: Component) {
        synchronized(this) {
            if (!this.dependencies.add(dependency)) {
                throw error("Already added $dependency to $name")
            }

            InjektPlugins.logger?.info("$name Add dependency $dependency")
        }
    }

    /**
     * Adds all of [scopeNames] to this component
     */
    fun scopeNames(scopeNames: Iterable<String>) {
        scopeNames.forEach {
            if (!this.scopeNames.add(it)) {
                error("Scope name $it was already added to $scopeNames")
            }
        }
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
                InjektPlugins.logger?.info("$name Create eager instance for ${it.value.binding}")
                it.value.get(this, null)
            }
    }

    private fun <T : Any> findInstance(key: Key, includeFactories: Boolean): Instance<T>? =
        synchronized(this) {
        var instance = instances[key]
            if (instance != null) return@synchronized instance as Instance<T>

        for (dependency in dependencies) {
            instance = dependency.findInstance<T>(key, false)
            if (instance != null) {
                return@synchronized instance
            }
        }

        // we search for generated factories as a last resort
        if (includeFactories) {
            try {
                val factory = FactoryFinder.find(key.type) ?: return null
                val binding = factory.create()
                return@synchronized addBindingInternal(binding) as Instance<T>
            } catch (e: ClassNotFoundException) {
                // ignore
            }
        }

            return@synchronized null
    }

    private fun addBindingInternal(binding: Binding<*>): Instance<*> {
        return synchronized(this) {
            val isOverride = bindings.remove(binding.key) != null

            if (isOverride && !binding.override) {
                throw OverrideException("Try to override binding $binding but was already in $name")
            }

            bindings[binding.key] = binding

            if (binding.scopeName != null && !scopeNames.contains(binding.scopeName)) {
                val parentWithScope = findComponentForScope(binding.scopeName)

                // add the binding to the parent
                if (parentWithScope != null) {
                    return@synchronized parentWithScope.addBindingInternal(binding)
                } else {
                    error("Component scope $name does not match binding scope ${binding.scopeName}")
                }
            }

            val instance = createInstance(binding)

            instances[binding.key] = instance

            InjektPlugins.logger?.let { logger ->
                val msg = if (isOverride) {
                    "$name Override $binding"
                } else {
                    "$name Declare $binding"
                }
                logger.debug(msg)
            }

            return@synchronized instance
        }
    }

    private fun <T : Any> createInstance(binding: Binding<T>): Instance<T> {
        val component = if (binding.scopeName != null) {
            findComponentForScope(binding.scopeName)
                ?: error("Cannot create instance for $binding unknown scope ${binding.scopeName}")
        } else {
            null
        }

        return when (binding.kind) {
            Binding.Kind.FACTORY -> FactoryInstance(binding, component)
            Binding.Kind.SINGLE -> SingleInstance(binding, component)
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