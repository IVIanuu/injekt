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

    private val setBindingsByName: MutableMap<String, MutableSet<Declaration<*>>> =
        ConcurrentHashMap()
    private val setBindingsByType: MutableMap<KClass<*>, MutableSet<Declaration<*>>> =
        ConcurrentHashMap()

    private val mapBindingsByName: MutableMap<String, MutableMap<Any, Declaration<*>>> =
        ConcurrentHashMap()
    private val mapBindingsByType: MutableMap<KClass<*>, MutableMap<Any, Declaration<*>>> =
        ConcurrentHashMap()

    /**
     * Adds all [Declaration]s of the [modules]
     */
    fun loadModules(vararg modules: Module) {
        modules.forEach { module ->
            module.component = component
            module.declarations.forEach {
                it.instance.component = component

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
     * Returns a [Set] of [Declaration]s matching [setType] and [setName]
     */
    fun getSetDeclarations(
        setType: KClass<*>,
        setName: String? = null
    ): Set<Declaration<*>> {
        return if (setName != null) {
            setBindingsByName[setName] ?: emptySet()
        } else {
            setBindingsByType[setType] ?: emptySet()
        }
    }

    /**
     * Returns a [Map] of [Declaration]s matching [mapType] and [mapName]
     */
    fun getMapDeclarations(
        mapType: KClass<*>,
        mapName: String? = null
    ): Map<Any, Declaration<*>> {
        return if (mapName != null) {
            mapBindingsByName[mapName] ?: emptyMap()
        } else {
            mapBindingsByType[mapType] ?: emptyMap()
        }
    }

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

        declaration.setBindings.forEach { (type, name) ->
            if (name != null) {
                setBindingsByName.getOrPut(name) { hashSetOf() }
                    .add(declaration)
            } else {
                setBindingsByType.getOrPut(type) { hashSetOf() }
                    .add(declaration)
            }
        }

        declaration.mapBindings.forEach { (type, key, name) ->
            if (name != null) {
                mapBindingsByName.getOrPut(name) { ConcurrentHashMap() }[key] = declaration
            } else {
                mapBindingsByType.getOrPut(type) { ConcurrentHashMap() }[key] = declaration
            }
        }
    }
}