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
class DeclarationRegistry(val name: String?) {

    val component get() = _component ?: error("Component not initialized")
    private var _component: Component? = null

    internal fun setComponent(component: Component) {
        if (_component != null) {
            error("Registries cannot be reused $name")
        }

        _component = component
    }

    private val declarations = mutableSetOf<Declaration<*>>()
    private val declarationsByName = mutableMapOf<String, Declaration<*>>()
    private val declarationsByType = mutableMapOf<KClass<*>, Declaration<*>>()
    private val createOnStartDeclarations = mutableSetOf<Declaration<*>>()

    /**
     * Adds all [Declaration]s of the [module]
     */
    fun addModule(module: Module) {
        module.setComponent(component)

        module.declarations.forEach {
            saveDeclaration(it, null)

            it.instance.setComponent(component)

            if (it.options.createOnStart) {
                createOnStartDeclarations.add(it)
            }
        }
    }

    /**
     * Adds all [Declaration]s of [dependency] to this component
     */
    fun addDependency(dependency: Component) {
        dependency.declarationRegistry.declarations.forEach {
            saveDeclaration(it, dependency)
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
        declarationsByName[name]
    } else {
        declarationsByType[type]
    }

    internal fun getEagerInstances(): Set<Declaration<*>> = createOnStartDeclarations

    private fun saveDeclaration(declaration: Declaration<*>, dependency: Component?) {
        val isOverride = declarations.remove(declaration)

        if (isOverride && !declaration.options.override) {
            throw OverrideException("${nameString()}Try to override declaration $declaration")
        }

        InjektPlugins.logger?.let { logger ->
            val kw = if (isOverride) "Override" else "Declare"
            val depString = if (dependency != null) {
                if (dependency.name != null) {
                    " from ${dependency.nameString()}"
                } else {
                    " from dependency "
                }
            } else {
                " "
            }
            logger.debug("${nameString()}$kw$depString$declaration")
        }

        declarations.add(declaration)

        if (declaration.name != null) {
            declarationsByName[declaration.name] = declaration
        } else {
            declaration.classes.forEach { declarationsByType[it] = declaration }
        }
    }
}