package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * The actual dependency container which provides definitions
 */
class Component internal constructor(val name: String?) {

    private val dependencies = hashSetOf<Component>()

    private val scopeNames = hashSetOf<String>()

    private val instances = hashMapOf<Key, Any>()
    private val definitions = hashMapOf<Key, BeanDefinition<*>>()

    /**
     * Returns a instance of [T] matching the [type], [name] and [parameters]
     */
    fun <T : Any> get(
        type: KClass<T>,
        name: String? = null,
        parameters: ParametersDefinition? = null
    ): T {
        val key = Key(type, name)
        return getByKey(key, parameters)
            ?: throw NoBeanDefinitionFoundException("$name Could not find definition for $key")
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
        if (definition.scopeId != null && !scopeNames.contains(definition.scopeId)) {
            val parentWithScope =
                dependencies.firstOrNull { it.scopeNames.contains(definition.scopeId) }

            // add the definition to the parent
            if (parentWithScope != null) {
                parentWithScope.addDefinition(definition)
                return
            } else {
                error("Component scope $name does not match definition scope ${definition.scopeId}")
            }
        }

        val isOverride = definitions.remove(definition.key) != null

        if (isOverride && !definition.override) {
            throw OverrideException("Try to override definition $definition but was already in $name")
        }

        definitions[definition.key] = definition

        InjektPlugins.logger?.let { logger ->
            val msg = if (isOverride) {
                "$name Override $definition"
            } else {
                "$name Declare $definition"
            }
            logger.debug(msg)
        }

        // create eager instances
        if (definition.eager) {
            InjektPlugins.logger?.info("$name Create eager instance for $definition")
            getByKey<Any>(definition.key, null)
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

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getByKey(key: Key, parameters: ParametersDefinition?): T? {
        return thisGetByKey(key, parameters)
            ?: getFromDependencyByKey(key, parameters)
    }

    private fun <T : Any> thisGetByKey(key: Key, parameters: ParametersDefinition?): T? {
        val definition = definitions[key] ?: return null

        @Suppress("UNCHECKED_CAST")
        return try {
            val instance = when (definition.kind) {
                BeanDefinition.Kind.FACTORY -> {
                    InjektPlugins.logger?.info("$name Create instance $definition")
                    definition.definition.invoke(
                        DefinitionContext(this),
                        parameters?.invoke() ?: emptyParameters()
                    )
                }
                BeanDefinition.Kind.SINGLE -> {
                    InjektPlugins.logger?.info("$name Return existing instance $definition")
                    instances[key] ?: (
                            definition.definition.invoke(
                                DefinitionContext(this),
                                parameters?.invoke() ?: emptyParameters()
                            ).also { instances[key] = it }).also {
                        InjektPlugins.logger?.info("$name Create instance $definition")
                    }
                }
            }

            instance as T
        } catch (e: Exception) {
            throw InstanceCreationException(
                "$name Couldn't instantiate $definition",
                e
            )
        }
    }

    private fun <T : Any> getFromDependencyByKey(key: Key, parameters: ParametersDefinition?): T? {
        for (dependency in dependencies) {
            val instance = dependency.getByKey<T>(key, parameters)
            if (instance != null) return instance
        }

        return null
    }

}