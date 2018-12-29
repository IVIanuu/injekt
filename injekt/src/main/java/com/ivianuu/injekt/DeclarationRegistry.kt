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

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Manages all [Declaration]s of a [Component]
 */
class DeclarationRegistry internal constructor(
    val name: String?,
    val component: Component
) {

    private val declarations = hashSetOf<Declaration<*>>()
    private val declarationsByName: MutableMap<String, Declaration<*>> = ConcurrentHashMap()
    private val declarationsByType: MutableMap<KClass<*>, Declaration<*>> = ConcurrentHashMap()
    private val createOnStartDeclarations = hashSetOf<Declaration<*>>()

    private val setBindings: MutableMap<String, MutableSet<Declaration<*>>> = ConcurrentHashMap()
    private val mapBindings: MutableMap<String, MutableMap<Any, Declaration<*>>> =
        ConcurrentHashMap()

    /**
     * Adds all [Declaration]s of the [modules]
     */
    fun loadModules(vararg modules: Module) {
        modules.forEach { module ->
            module.component = component
            module.declarations.forEach {
                saveDeclaration(it)

                if (it.options.createOnStart) {
                    createOnStartDeclarations.add(it)
                }
            }
        }
    }

    /**
     * Adds all [Declaration]s of [dependencies] to this component
     */
    fun loadDependencies(vararg dependencies: Component) {
        dependencies.forEach { dependency ->
            dependency.declarationRegistry.setBindings.forEach { (key, value) ->
                setBindings.getOrPut(key) { linkedSetOf() }
                    .addAll(value)
            }

            dependency.declarationRegistry.mapBindings.forEach { (key, map) ->
                mapBindings.getOrPut(key) { ConcurrentHashMap() }
                    .putAll(map)
            }

            dependency.declarationRegistry.getAllDeclarations().forEach { declaration ->
                saveDeclaration(declaration)
            }
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

    /**
     * Saves the [declaration]
     */
    fun saveDeclaration(declaration: Declaration<*>) {
        val isOverride = declarations.remove(declaration)

        if (isOverride && !declaration.options.override) {
            throw OverrideException("${nameString()}Try to override declaration $declaration")
        }

        declaration.instance.component = component

        InjektPlugins.logger?.let { logger ->
            val kw = if (isOverride) "Override" else "Declare"
            logger.debug("${nameString()}$kw $declaration")
        }

        declarations.add(declaration)

        if (declaration.name != null) {
            declarationsByName[declaration.name] = declaration
        } else {
            declaration.classes.forEach { declarationsByType[it] = declaration }
        }

        declaration.setBindings.forEach { setName ->
            setBindings.getOrPut(setName) { linkedSetOf() }.add(declaration)

            saveDeclaration(
                Declaration.create(
                    MultiBindingSet::class,
                    setName,
                    Declaration.Kind.FACTORY
                ) { MultiBindingSet(setBindings[setName]!! as Set<Declaration<Any>>) }.apply {
                    options.override = true
                }
            )
        }

        declaration.mapBindings.forEach { (mapName, key) ->
            mapBindings.getOrPut(mapName) { ConcurrentHashMap() }[key] = declaration

            saveDeclaration(
                Declaration.create(
                    MultiBindingMap::class,
                    mapName,
                    Declaration.Kind.FACTORY
                ) { MultiBindingMap(mapBindings[mapName]!! as Map<Any, Declaration<Any>>) }.apply {
                    options.override = true
                }
            )
        }
    }
}