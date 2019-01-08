package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger
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
     * Whether or not contains the [dependency]
     */
    fun containsDependency(dependency: Component): Boolean =
        dependencies.contains(dependency)

    /**
     * Removes the given dependency
     */
    fun removeDependency(dependency: Component) {
        dependencies.remove(dependency)
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

    fun removeScopeName(scopeName: String) {
        scopeNames.remove(scopeName)
    }

    /**
     * Whether or not contains [scopeName]
     */
    fun containsScopeName(scopeName: String): Boolean = scopeNames.contains(scopeName)

    /**
     * Returns all [BeanDefinition]s added to this component
     */
    fun getDefinitions(): Set<BeanDefinition<*>> = definitions.values.toSet()

    /**
     * Saves the [definition]
     */
    fun addDefinition(definition: BeanDefinition<*>) {
        val isOverride = definitions.remove(definition.key) != null

        if (isOverride && !definition.override) {
            throw OverrideException("Try to override definition $definition but was already in $name")
        }

        definitions[definition.key] = definition

        if (definition.scopeName != null && !scopeNames.contains(definition.scopeName)) {
            val parentWithScope =
                dependencies.firstOrNull { it.scopeNames.contains(definition.scopeName) }

            // add the definition to the parent
            if (parentWithScope != null) {
                parentWithScope.addDefinition(definition)
                return
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
    }

    /**
     * Removes the given [definition] and any instance associated with it
     */
    fun removeDefinition(definition: BeanDefinition<*>) {
        definitions.remove(definition.key)
        instances.remove(definition.key)
    }

    /**
     * Whether or not contains the [definition]
     */
    fun containsDefinition(definition: BeanDefinition<*>): Boolean =
        definitions.containsKey(definition.key)

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
        val instance = instances[key]
        if (instance != null) return instance as Instance<T>

        for (dependency in dependencies) {
            val instance = dependency.findInstance<T>(key, false)
            if (instance != null) return instance
        }

        // we search for generated factories as a last resort
        if (includeFactories) {
            try {
                val factoryType = Class.forName(key.type.java.name + "__Factory")
                val factory = factoryType.newInstance() as DefinitionFactory<T>

                logger?.info("Found definition factory for $key")

                val definition = factory.create()
                addDefinition(definition)

                // if we reach here we got our definition
                // search again for now
                return findInstance(key, false)
            } catch (e: ClassNotFoundException) {
                // ignore
            }
        }

        return null
    }

    private fun <T : Any> BeanDefinition<T>.createInstance() = when (kind) {
        BeanDefinition.Kind.FACTORY -> FactoryInstance(this, this@Component)
        BeanDefinition.Kind.SINGLE -> SingleInstance(this, this@Component)
    }
}