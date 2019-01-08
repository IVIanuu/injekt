package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * The actual dependency container which provides definitions
 */
class Component internal constructor(val name: String?) {

    private val dependencies = hashSetOf<Component>()
    private val scopeNames = hashSetOf<String>()
    private val definitions = hashMapOf<Key, BeanDefinition<*>>()

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
        return findInstance<T>(key, true)?.get(parameters)
            ?: throw NoBeanDefinitionFoundException("${this.name} Could not find definition for $key")
    }

    /**
     * Adds all [BeanDefinition]s of the [modules]
     */
    fun modules(modules: Iterable<Module>) {
        modules.forEach { module ->
            InjektPlugins.logger?.info("$name load module ${module.name}")
            module.definitions.forEach { addDefinition(it.value) }
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
        if (!this.dependencies.add(dependency)) {
            throw error("Already added $dependency to $name")
        }

        instances.putAll(dependency.instances)

        InjektPlugins.logger?.info("$name Add dependency $dependency")
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
     * Returns all [BeanDefinition]s added to this component
     */
    fun getDefinitions(): Set<BeanDefinition<*>> = definitions.values.toSet()

    /**
     * Saves the [definition]
     */
    fun addDefinition(definition: BeanDefinition<*>) {
        addDefinitionInternal(definition)
    }

    /**
     * Creates all eager instances of this component
     */
    fun createEagerInstances() {
        instances
            .filter { it.value.definition.eager && !it.value.isCreated }
            .forEach {
                InjektPlugins.logger?.info("$name Create eager instance for ${it.value.definition}")
                it.value.get()
            }
    }

    private fun <T : Any> findInstance(key: Key, includeFactories: Boolean): Instance<T>? {
        var instance = instances[key]
        if (instance != null) return instance as Instance<T>

        for (dependency in dependencies) {
            instance = dependency.findInstance<T>(key, false)
            if (instance != null) {
                instances[key] = instance
                return instance
            }
        }

        // we search for generated factories as a last resort
        if (includeFactories) {
            try {
                val factory = FactoryFinder.find(key.type) ?: return null

                val definition = factory.create()
                return addDefinitionInternal(definition) as Instance<T>
            } catch (e: ClassNotFoundException) {
                // ignore
            }
        }

        return null
    }

    private fun addDefinitionInternal(definition: BeanDefinition<*>): Instance<*> {
        val isOverride = definitions.remove(definition.key) != null

        if (isOverride && !definition.override) {
            throw OverrideException("Try to override definition $definition but was already in $name")
        }

        definitions[definition.key] = definition

        if (definition.scopeName != null && !scopeNames.contains(definition.scopeName)) {
            val parentWithScope = findComponentForScope(definition.scopeName)

            // add the definition to the parent
            if (parentWithScope != null) {
                val instance = parentWithScope.addDefinitionInternal(definition)
                instances[definition.key] = instance
                return instance
            } else {
                error("Component scope $name does not match definition scope ${definition.scopeName}")
            }
        }

        val instance = definition.createInstance()

        instances[definition.key] = instance

        InjektPlugins.logger?.let { logger ->
            val msg = if (isOverride) {
                "$name Override $definition"
            } else {
                "$name Declare $definition"
            }
            logger.debug(msg)
        }

        return instance
    }

    private fun <T : Any> BeanDefinition<T>.createInstance() = when (kind) {
        BeanDefinition.Kind.FACTORY -> FactoryInstance(this, this@Component)
        BeanDefinition.Kind.SINGLE -> SingleInstance(this, this@Component)
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