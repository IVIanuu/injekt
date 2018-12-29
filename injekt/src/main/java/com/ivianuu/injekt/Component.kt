package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger
import kotlin.reflect.KClass

/**
 * The actual dependency container which provides declarations
 */
class Component internal constructor(val name: String?) {

    val declarationRegistry = DeclarationRegistry(name).apply {
        setComponent(this@Component)
    }

    /**
     * Adds all [Declaration]s of the [module]
     */
    fun addModule(module: Module) {
        measureDurationOnly {
            declarationRegistry.addModule(module)
        }.let {
            InjektPlugins.logger?.debug("${nameString()}Adding module ${module.nameString()}took $it ms")
        }
    }

    /**
     * Adds all [Declaration]s of [dependency] to this component
     */
    fun addDependency(dependency: Component) {
        measureDurationOnly {
            declarationRegistry.addDependency(dependency)
        }.let {
            InjektPlugins.logger?.debug("${nameString()}Adding dependency ${dependency.nameString()}took $it ms")
        }
    }

    /**
     * Instantiates all eager instances
     */
    fun createEagerInstances(params: ParamsDefinition? = null) {
        measureDurationOnly {
            declarationRegistry.getEagerInstances().forEach { it.resolveInstance(params) }
        }.let {
            logger?.debug("${nameString()}Instantiating eager instances took $it ms")
        }
    }

    /**
     * Returns a instance of [T] matching the [type], [name] and [params]
     */
    fun <T : Any> get(
        type: KClass<T>,
        name: String? = null,
        params: ParamsDefinition? = null
    ) = synchronized(this) {
        val declaration = declarationRegistry.findDeclaration(type, name)

        if (declaration != null) {
            @Suppress("UNCHECKED_CAST")
            declaration.resolveInstance(params) as T
        } else {
            throw InjectionException("${nameString()}Could not find declaration for ${type.java.name + name.orEmpty()}")
        }
    }


}