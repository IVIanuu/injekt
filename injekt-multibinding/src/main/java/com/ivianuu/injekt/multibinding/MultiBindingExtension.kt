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

package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentExtension
import com.ivianuu.injekt.InjektPlugins
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.OverrideException
import com.ivianuu.injekt.getOrDefault
import com.ivianuu.injekt.registerComponentExtension

/**
 * [ComponentExtension] to enable multi binding functionality
 */
class MultiBindingExtension : ComponentExtension {

    override fun onBindingAdded(component: Component, binding: Binding<*>) {
        handleMap(component, binding)
        handleSet(component, binding)
    }

    private fun handleMap(component: Component, binding: Binding<*>) {
        val mapBindings = binding.attributes.get<Map<String, MapBinding>>(KEY_MAP_BINDINGS)
            ?: return

        mapBindings
            .filter { (mapName) -> component.getBindings().none { it.name == mapName } }
            .forEach { (_, mapBinding) -> declareMapBinding(component, mapBinding.mapName) }
    }

    private fun declareMapBinding(component: Component, mapName: String) {
        val multiMapBinding = Binding.createFactory(
            type = MultiBindingMap::class,
            name = mapName,
            definition = {
                val allMapBindings = component.getAllBindings()
                    .mapNotNull { binding ->
                        binding.attributes.get<Map<String, MapBinding>>(KEY_MAP_BINDINGS)
                            ?.get(mapName)?.let { binding to it }
                    }

                val existingKeys = mutableSetOf<Any>()

                // check overrides
                allMapBindings.forEach { (_, mapBinding) ->
                    if (!existingKeys.add(mapBinding.key) && !mapBinding.override) {
                        throw OverrideException("Try to override ${mapBinding.key} in map binding $mapBinding")
                    }
                }

                allMapBindings
                    .map { it.second.key to it.first }
                    .toMap()
                    .mapValues { it.value as Binding<Any> }
                    .let { MultiBindingMap(component, it) }
            }
        )

        component.addBinding(multiMapBinding)
    }

    private fun handleSet(component: Component, binding: Binding<*>) {
        val setBindings = binding.attributes.get<Map<String, SetBinding>>(KEY_SET_BINDINGS)
            ?: return

        setBindings
            .filter { (setName) -> component.getBindings().none { it.name == setName } }
            .forEach { (_, setBinding) -> declareSetBinding(component, setBinding.setName) }
    }

    private fun declareSetBinding(component: Component, setName: String) {
        val multiSetBinding = Binding.createFactory(
            type = MultiBindingSet::class,
            name = setName,
            definition = {
                val allSetBindings = component.getAllBindings()
                    .mapNotNull { binding ->
                        binding.attributes.get<Map<String, SetBinding>>(KEY_SET_BINDINGS)
                            ?.get(setName)?.let { binding to it }
                    }

                val existingKeys = mutableSetOf<Key>()

                // check overrides
                allSetBindings.forEach { (binding, setBinding) ->
                    val key = binding.attributes.getOrDefault(KEY_ORIGINAL_KEY) { binding.key }
                    if (!existingKeys.add(key) && !setBinding.override) {
                        throw OverrideException("Try to override $key in set binding $setBinding")
                    }
                }

                allSetBindings
                    .map { it.first as Binding<Any> }
                    .toSet()
                    .let { MultiBindingSet(component, it) }
            }
        )

        component.addBinding(multiSetBinding)
    }
}

/**
 * Adds the [MultiBindingExtension]
 */
fun InjektPlugins.multiBindingExtension() {
    registerComponentExtension(MultiBindingExtension())
}