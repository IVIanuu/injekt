package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ComponentContext(private val component: Component) {

    private val dependencies = hashSetOf<Component>()
    private val definitions = hashMapOf<Key, BeanDefinition<*>>()
    private val instances = hashMapOf<Key, Any>()

    /**
     * Adds all [BeanDefinition]s of the [modules]
     */
    fun loadModules(modules: Iterable<Module>) {
        modules.forEach { module ->
            InjektPlugins.logger?.info("${component.name} load module ${module.name}")
            module.definitions.forEach { saveDefinition(it.value) }
        }
    }

    /**
     * Returns all direct dependencies of this component
     */
    fun getDependencies(): Set<Component> = dependencies

    /**
     * Adds all of [dependencies] as dependencies
     */
    fun addDependencies(dependencies: Iterable<Component>) {
        dependencies.forEach { addDependency(it) }
    }

    /**
     * Adds the [dependency] as a dependency
     */
    fun addDependency(dependency: Component) {
        if (!this.dependencies.add(dependency)) {
            throw error("Already added ${dependency.name} to ${component.name}")
        }

        logger?.info("${component.name} added dependency ${dependency.name}")
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

    fun <T : Any> get(
        key: Key,
        parameters: ParametersDefinition? = null
    ): T {
        return getInstanceOrNull(key, parameters)
            ?: throw NoBeanDefinitionFoundException("${component.name} Could not find definition for $key")
    }

    /**
     * Returns all [BeanDefinition]s added to this component
     */
    fun getDefinitions(): Set<BeanDefinition<*>> = definitions.values.toSet()

    /**
     * Saves the [definition]
     */
    fun saveDefinition(definition: BeanDefinition<*>) {
        val isOverride = definitions.remove(definition.key) != null

        if (isOverride && !definition.override) {
            throw OverrideException("Try to override definition $definition but was already in ${component.name}")
        }

        definitions[definition.key] = definition

        InjektPlugins.logger?.let { logger ->
            val msg = if (isOverride) {
                "${component.name} Override $definition"
            } else {
                "${component.name} Declare $definition"
            }
            logger.debug(msg)
        }

        // create eager instances
        if (definition.eager) {
            InjektPlugins.logger?.info("${component.name} Create eager instance for $definition")
            getInstanceOrNull<Any>(definition.key, null)
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
    private fun <T : Any> getInstanceOrNull(key: Key, parameters: ParametersDefinition?): T? {
        // fast look up
        val existingInstance = instances[key]
        if (existingInstance != null) return existingInstance as T

        val definition = definitions[key]

        if (definition != null) {
            try {
                return when (definition.kind) {
                    BeanDefinition.Kind.FACTORY -> {
                        definition.definition.invoke(
                            DefinitionContext(component),
                            parameters?.invoke() ?: emptyParameters()
                        ) as T
                    }
                    BeanDefinition.Kind.SINGLE -> {
                        definition.definition.invoke(
                            DefinitionContext(component),
                            parameters?.invoke() ?: emptyParameters()
                        ).also { instances[key] = it } as T
                    }
                }
            } catch (e: Exception) {
                throw InstanceCreationException(
                    "${component.context} Couldn't instantiate $definition",
                    e
                )
            }
        } else {
            for (dependency in dependencies) {
                val instance = dependency.context.getInstanceOrNull<T>(key, parameters)
                if (instance != null) return instance
            }
        }

        return null
    }
}