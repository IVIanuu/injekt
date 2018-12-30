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

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MultiBindingRegistry(
    private val component: Component
) {

    lateinit var declarationRegistry: DeclarationRegistry

    private val setBindings: MutableMap<String, MutableSet<Declaration<*>>> =
        ConcurrentHashMap()
    private val mapBindings: MutableMap<String, MutableMap<Any, Declaration<*>>> =
        ConcurrentHashMap()

    /**
     * Returns all [Declaration]s which are bound into a set
     */
    fun getAllSetBindings(): Map<String, Set<Declaration<*>>> = setBindings

    /**
     * Returns all [Declaration]s which are bound into a map
     */
    fun getAllMapBindings(): Map<String, Map<Any, Declaration<*>>> = mapBindings

    /**
     * Loads all map and set bindings of the [modules]
     */
    fun loadModules(vararg modules: Module) {
        modules.forEach { module ->
            module.setBindings.forEach { saveSetMultiBinding(it) }
            module.mapBindings.forEach { saveMapMultiBinding(it) }
        }
    }

    /**
     * Loads all map and set bindings of the [components]
     */
    fun loadComponents(vararg components: Component) {
        components.forEach { component ->
            component.multiBindingRegistry.setBindings.forEach { saveSetMultiBinding(it.key) }
            component.multiBindingRegistry.mapBindings.forEach { saveMapMultiBinding(it.key) }
        }
    }

    /**
     * Saves multi bindings of [declaration]
     */
    fun saveMultiBindingsForDeclaration(declaration: Declaration<*>) {
        declaration.setBindings.forEach { name ->
            val binding =
                setBindings[name] ?: error("No set multi binding found for $name $declaration")
            binding.add(declaration)
        }

        declaration.mapBindings.forEach { (name, key) ->
            val binding =
                mapBindings[name] ?: error("No map multi binding found for $name $declaration")
            if (binding.containsKey(key)) {
                error("Keys must be unique but added twice $key")
            }
            binding[key] = declaration
        }
    }

    /**
     * Registers a set multi binding for [name]
     */
    fun saveSetMultiBinding(name: String) {
        if (setBindings.contains(name)) return
        setBindings[name] = linkedSetOf()

        val declaration = Declaration.create(
            MultiBindingSet::class,
            name,
            Declaration.Kind.FACTORY
        ) {
            MultiBindingSet(
                setBindings[name]
                        as? Set<Declaration<Any>> ?: emptySet()
            )
        }.apply {
            options.override = true
        }

        declarationRegistry.saveDeclaration(declaration)
    }

    /**
     * Registers a map multi binding for [name]
     */
    fun saveMapMultiBinding(name: String) {
        if (mapBindings.contains(name)) return
        mapBindings[name] = hashMapOf()

        val declaration = Declaration.create(
            MultiBindingMap::class,
            name,
            Declaration.Kind.FACTORY
        ) {
            MultiBindingMap(
                mapBindings[name]
                        as? Map<Any, Declaration<Any>> ?: emptyMap()
            )
        }.apply {
            options.override = true
        }

        declarationRegistry.saveDeclaration(declaration)
    }
}