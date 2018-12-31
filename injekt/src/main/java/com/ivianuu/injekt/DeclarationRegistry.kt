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
    fun loadModules(vararg modules: Module) {
        modules.forEach { module ->
            module.declarations
                .map { it.copyIdentity() }
                .forEach {
                    saveDeclaration(it)
                    if (it.createOnStart) {
                        createOnStartDeclarations.add(it)
                    }
                }
        }
    }

    /**
     * Adds all [Declaration]s of [components] to this component
     */
    fun loadComponents(vararg components: Component) {
        components.forEach { component ->
            component.declarationRegistry.getAllDeclarations().forEach {
                saveDeclaration(it)
            }
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
    ): Declaration<*>? = declarations[Key(type, name)]

    internal fun getEagerInstances(): Set<Declaration<*>> = createOnStartDeclarations

    /**
     * Saves the [declaration]
     */
    fun saveDeclaration(declaration: Declaration<*>) {
        val oldDeclaration = declarations[declaration.key]
        val isOverride = oldDeclaration != null
        if (isOverride && !declaration.override) {
            throw OverrideException("Try to override declaration $declaration but was already saved $oldDeclaration")
        }

        declarations[declaration.key] = declaration

        declaration.instance.component = component

        InjektPlugins.logger?.let { logger ->
            val kw = if (isOverride) "Override" else "Declare"
            logger.debug("$kw $declaration")
        }
    }
}