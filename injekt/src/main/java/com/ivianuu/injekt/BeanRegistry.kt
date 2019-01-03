/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger
import kotlin.reflect.KClass

/**
 * Manages all [BeanDefinition]s of a [Component]
 */
class BeanRegistry internal constructor(val component: Component) {

    private val definitions = hashSetOf<BeanDefinition<*>>()
    private val definitionNames = hashMapOf<String, BeanDefinition<*>>()
    private val definitionTypes = hashMapOf<KClass<*>, BeanDefinition<*>>()
    private val createOnStartDefinitions = hashSetOf<BeanDefinition<*>>()

    /**
     * Adds all [BeanDefinition]s of the [modules]
     */
    fun loadModules(vararg modules: Module, dropOverrides: Boolean = false) {
        modules.forEach { module ->
            logger?.info("${component.name} load module ${module.name}")
            module.getDefinitions()
                .forEach { saveDefinition(it, dropOverrides) }
        }
    }

    /**
     * Adds all current [BeanDefinition]s of the [components]
     */
    fun linkComponents(vararg components: Component, dropOverrides: Boolean = false) {
        components.forEach { component ->
            logger?.info("${component.name} link component ${component.name}")
            component.beanRegistry.definitions
                .forEach { linkDefinition(it, dropOverrides) }
        }
    }

    /**
     * Returns all [BeanDefinition]s
     */
    fun getAllDefinitions(): Set<BeanDefinition<*>> = definitions

    /**
     * Returns the [BeanDefinition] for [type] and [name] or null
     */
    fun findDefinition(
        type: KClass<*>,
        name: String? = null
    ): BeanDefinition<*>? = if (name != null) {
        definitionNames[name]
    } else {
        definitionTypes[type]
    }

    /**
     * Whether or not contains [type] and [name]
     */
    fun containsDefinition(type: KClass<*>, name: String? = null) =
        findDefinition(type, name) != null

    /**
     * Saves the [definition] which was not added to [Component] yet
     */
    fun saveDefinition(
        definition: BeanDefinition<*>,
        dropOverrides: Boolean = false
    ) {
        addDefinition(definition, dropOverrides, false)
    }

    /**
     * Saves a [definition] from another component
     */
    fun linkDefinition(
        definition: BeanDefinition<*>,
        dropOverrides: Boolean = false
    ) {
        addDefinition(definition, dropOverrides, true)
    }

    fun removeDefinition(type: KClass<*>, name: String? = null) {
        findDefinition(type, name)?.let { removeDefinition(it) }
    }

    fun removeDefinition(definition: BeanDefinition<*>) {
        if (definition.name != null) {
            definitionNames.remove(definition.name)
        } else {
            definitionTypes.remove(definition.type)
        }
    }

    internal fun getEagerInstances(): Set<BeanDefinition<*>> = createOnStartDefinitions

    private fun addDefinition(
        definition: BeanDefinition<*>,
        dropOverrides: Boolean,
        fromComponent: Boolean
    ) {
        val oldDefinition = if (definition.name != null) {
            definitionNames[definition.name]
        } else {
            definitionTypes[definition.type]
        }

        val isOverride = oldDefinition != null

        if (isOverride && !definition.override) {
            if (dropOverrides) {
                logger?.info("${component.name} Drop override $definition")
                return
            } else {
                throw OverrideException("Try to override definition $definition but was already saved $oldDefinition to ${component.name}")
            }
        }

        if (definition.name != null) {
            definitionNames[definition.name] = definition
        } else {
            definitionTypes[definition.type] = definition
        }

        definitions.add(definition)

        if (!fromComponent) {
            definition.instance.component = component

            if (definition.createOnStart) {
                createOnStartDefinitions.add(definition)
            }
        }

        InjektPlugins.logger?.let { logger ->
            val msg = if (isOverride) {
                "${component.name} Override $definition"
            } else {
                if (fromComponent) {
                    "${component.name} Link $definition from ${definition.instance.component.name}"
                } else {
                    "${component.name} Declare $definition"
                }
            }
            logger.debug(msg)
        }
    }
}