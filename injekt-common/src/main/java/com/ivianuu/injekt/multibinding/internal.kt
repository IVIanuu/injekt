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
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.bridge.KEY_ORIGINAL_KEY
import com.ivianuu.injekt.getOrDefault

internal fun <K, V> Component.getMultiBindingMap(mapName: MapName<K, V>): Map<K, Binding<V>> {
    val allMapBindings = getAllBindings()
        .mapNotNull { binding ->
            binding.attributes.get<Map<MapName<*, *>, MapBinding<*, *>>>(KEY_MAP_BINDINGS)
                ?.get(mapName)
                ?.let { binding to it }
        }

    val mapBindingsToUse = linkedMapOf<Any?, Binding<*>>()

    // check overrides
    allMapBindings.forEach { (binding, mapBinding) ->
        val isOverride = mapBindingsToUse.remove(mapBinding.key) != null

        if (isOverride && !mapBinding.override) {
            throw IllegalStateException("Try to override ${mapBinding.key} in map binding $mapBinding")
        }

        mapBindingsToUse[mapBinding.key] = binding
    }

    return mapBindingsToUse as Map<K, Binding<V>>
}

internal fun <T> Component.getMultiBindingSet(setName: SetName<T>): Set<Binding<T>> {
    val allSetBindings = getAllBindings()
        .mapNotNull { binding ->
            binding.attributes.get<Map<SetName<*>, SetBinding<*>>>(KEY_SET_BINDINGS)
                ?.get(setName)?.let { binding to it }
        }

    val setBindingsToUse = linkedMapOf<Key, Binding<*>>()

    // check overrides
    allSetBindings.forEach { (binding, setBinding) ->
        val key = binding.attributes.getOrDefault(KEY_ORIGINAL_KEY) { binding.key }

        val isOverride = setBindingsToUse.remove(binding.key) != null

        if (isOverride && !setBinding.override) {
            throw IllegalStateException("Try to override $key in set binding $setBinding")
        }

        setBindingsToUse[binding.key] = binding
    }


    return setBindingsToUse.values.toSet() as Set<Binding<T>>
}

internal fun Component.getAllBindings(): List<Binding<*>> =
    arrayListOf<Binding<*>>().also { collectBindings(it) }

internal fun Component.collectBindings(
    bindings: MutableList<Binding<*>>
) {
    dependencies.forEach { it.collectBindings(bindings) }
    bindings.addAll(this.bindings.values)
}