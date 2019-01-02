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

    private val declarations = hashMapOf<Key, Declaration<*>>()
    private val createOnStartDeclarations = hashSetOf<Declaration<*>>()

    /**
     * Adds all [Declaration]s of the [modules]
     */
    fun loadModules(vararg modules: Module, dropOverrides: Boolean = false) {
        modules.forEach { module ->
            logger?.info("${component.name} load module ${module.name}")

            module.getDeclarations(component.context)
                .forEach {
                    saveDeclaration(it.value, dropOverrides)

                    it.value.instance.context = component.context

                    if (it.value.createOnStart) {
                        createOnStartDeclarations.add(it.value)
                    }
                }
        }
    }

    /**
     * Adds all [Declaration]s of the [components]
     */
    fun loadComponents(vararg components: Component, dropOverrides: Boolean = false) {
        components.forEach { component ->
            logger?.info("${component.name} load component ${component.name}")

            component.declarationRegistry.declarations
                .forEach { saveDeclaration(it.value, dropOverrides) }
        }
    }

    /**
     * Returns all [Declaration]s
     */
    fun getAllDeclarations(): Set<Declaration<*>> = declarations.values.toSet()

    /**
     * Returns the [Declaration] for [type] and [name] or null
     */
    fun findDeclaration(
        type: KClass<*>,
        name: String? = null
    ): Declaration<*>? = declarations[Key.of(type, name)]

    /**
     * Returns the [Declaration] for [type] and [name] or null
     */
    fun findDeclaration(key: Key): Declaration<*>? = declarations[key]

    internal fun getEagerInstances(): Set<Declaration<*>> = createOnStartDeclarations

    /**
     * Saves the [declaration]
     */
    fun saveDeclaration(
        declaration: Declaration<*>,
        dropOverrides: Boolean = false
    ) {
        val key = declaration.key
        val oldDeclaration = declarations[key]
        val isOverride = oldDeclaration != null
        if (isOverride && !declaration.override) {
            if (dropOverrides) {
                return
            } else {
                throw OverrideException("Try to override declaration $declaration but was already saved $oldDeclaration to ${component.name}")
            }
        }

        declarations[key] = declaration

        InjektPlugins.logger?.let { logger ->
            val kw = if (isOverride) "Override" else "Declare"
            logger.debug("${component.name} $kw $declaration")
        }
    }
}