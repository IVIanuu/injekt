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
 * Manages all [Declaration]s of a [Component]
 */
class DeclarationRegistry internal constructor(val component: Component) {

    private val declarations = hashSetOf<Declaration<*>>()
    private val declarationNames = hashMapOf<String, Declaration<*>>()
    private val declarationTypes = hashMapOf<KClass<*>, Declaration<*>>()
    private val createOnStartDeclarations = hashSetOf<Declaration<*>>()

    /**
     * Adds all [Declaration]s of the [modules]
     */
    fun loadModules(vararg modules: Module, dropOverrides: Boolean = false) {
        modules.forEach { module ->
            logger?.info("${component.name} load module ${module.name}")

            module.getDeclarations(component.context)
                .forEach { saveDeclarationInternal(it, dropOverrides, false) }
        }
    }

    /**
     * Adds all current [Declaration]s of the [components]
     */
    fun linkComponents(vararg components: Component, dropOverrides: Boolean = false) {
        components.forEach { component ->
            logger?.info("${component.name} link component ${component.name}")

            component.declarationRegistry.declarations
                .forEach { saveDeclarationInternal(it, dropOverrides, true) }
        }
    }

    /**
     * Returns all [Declaration]s
     */
    fun getAllDeclarations(): Set<Declaration<*>> = declarations

    /**
     * Returns the [Declaration] for [type] and [name] or null
     */
    fun findDeclaration(
        type: KClass<*>,
        name: String? = null
    ): Declaration<*>? = if (name != null) {
        declarationNames[name]
    } else {
        declarationTypes[type]
    }

    internal fun getEagerInstances(): Set<Declaration<*>> = createOnStartDeclarations

    /**
     * Saves the [declaration]
     */
    fun saveDeclaration(
        declaration: Declaration<*>,
        dropOverrides: Boolean = false
    ) {
        saveDeclarationInternal(declaration, dropOverrides, false)
    }

    private fun saveDeclarationInternal(
        declaration: Declaration<*>,
        dropOverrides: Boolean,
        fromComponent: Boolean
    ) {

        val oldDeclaration = if (declaration.name != null) {
            declarationNames[declaration.name]
        } else {
            declarationTypes[declaration.type]
        }
        val isOverride = oldDeclaration != null
        if (isOverride && !declaration.override) {
            if (dropOverrides) {
                logger?.info("${component.name} Drop override $declaration")
                return
            } else {
                throw OverrideException("Try to override declaration $declaration but was already saved $oldDeclaration to ${component.name}")
            }
        }

        if (declaration.name != null) {
            declarationNames[declaration.name] = declaration
        } else {
            declarationTypes[declaration.type] = declaration
        }

        if (!fromComponent) {
            declaration.instance.context = component.context

            if (declaration.createOnStart) {
                createOnStartDeclarations.add(declaration)
            }
        }

        InjektPlugins.logger?.let { logger ->
            val msg = if (isOverride) {
                "${component.name} Override $declaration"
            } else {
                if (fromComponent) {
                    "${component.name} Link $declaration from ${declaration.instance.context.component.name}"
                } else {
                    "${component.name} Declare $declaration"
                }
            }
            logger.debug(msg)
        }
    }
}