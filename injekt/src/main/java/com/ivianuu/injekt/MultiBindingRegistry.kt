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
class MultiBindingRegistry(private val component: Component) {

    lateinit var declarationRegistry: DeclarationRegistry

    private val setBindings = hashSetOf<SetBinding>()
    private val setBindingsByName: MutableMap<String, SetBinding> =
        ConcurrentHashMap()

    private val mapBindings = hashSetOf<MapBinding>()
    private val mapBindingsByName: MutableMap<String, MapBinding> =
        ConcurrentHashMap()

    private val declarationsBySet: MutableMap<String, MutableSet<Declaration<*>>> =
        ConcurrentHashMap()
    private val declarationsByMap: MutableMap<String, MutableMap<Any, Declaration<*>>> =
        ConcurrentHashMap()

    /**
     * Returns all [Declaration]s which are bound into a set
     */
    fun getAllSetDeclarations(): Map<String, Set<Declaration<*>>> = declarationsBySet

    /**
     * Returns all [Declaration]s which are bound into a map
     */
    fun getAllMapDeclarations(): Map<String, Map<Any, Declaration<*>>> = declarationsByMap

    fun loadModules(vararg modules: Module) {
        modules.forEach { module ->
            module.setBindings.forEach { saveSetMultiBinding(it) }
            module.mapBindings.forEach { saveMapMultiBinding(it) }
        }
    }

    fun loadComponents(vararg components: Component) {
        components.forEach { component ->
            component.multiBindingRegistry.setBindings.forEach { saveSetMultiBinding(it) }
            component.multiBindingRegistry.mapBindings.forEach { saveMapMultiBinding(it) }
        }
    }

    fun saveMultiBindingsForDeclaration(declaration: Declaration<*>) {
        declaration.setBindings.forEach { bindingName ->
            val binding = setBindingsByName[bindingName]
                ?: throw error("No set multi binding found for $bindingName $declaration")

            if (!binding.type.java.isAssignableFrom(declaration.primaryType.java)) {
                error("type ${declaration.primaryType.getFullName()} is not assignable from set binding type ${binding.type.getFullName()} $declaration")
            }

            declarationsBySet.getOrPut(bindingName) { linkedSetOf() }
                .add(declaration)
        }

        declaration.mapBindings.forEach { (bindingName, key) ->
            val binding = mapBindingsByName[bindingName]
                ?: throw error("No map multi binding found for $bindingName $declaration")

            if (!binding.type.java.isAssignableFrom(declaration.primaryType.java)) {
                error("type ${declaration.primaryType.getFullName()} is not assignable from map binding type ${binding.type.getFullName()} $declaration")
            }

            if (!binding.keyType.java.isAssignableFrom(key::class.java)) {
                error("key type ${key::class.getFullName()} is not assignable from map binding key type ${binding.keyType.getFullName()} $declaration")
            }

            declarationsByMap.getOrPut(bindingName) { ConcurrentHashMap() }[key] = declaration
        }
    }

    fun saveSetMultiBinding(binding: SetBinding) {
        if (setBindings.contains(binding)) return
        setBindings.add(binding)
        setBindingsByName[binding.name] = binding

        val declaration = Declaration.create(
            MultiBindingSet::class,
            binding.name,
            Declaration.Kind.FACTORY
        ) {
            MultiBindingSet(
                declarationsBySet[binding.name]
                        as? Set<Declaration<Any>> ?: emptySet()
            )
        }.apply {
            options.override = true
        }

        declarationRegistry.saveDeclaration(declaration)
    }

    fun saveMapMultiBinding(binding: MapBinding) {
        if (mapBindings.contains(binding)) return
        mapBindings.add(binding)
        mapBindingsByName[binding.name] = binding

        val declaration = Declaration.create(
            MultiBindingMap::class,
            binding.name,
            Declaration.Kind.FACTORY
        ) {
            MultiBindingMap(
                declarationsByMap[binding.name]
                        as? Map<Any, Declaration<Any>> ?: emptyMap()
            )
        }.apply {
            options.override = true
        }

        declarationRegistry.saveDeclaration(declaration)
    }
}