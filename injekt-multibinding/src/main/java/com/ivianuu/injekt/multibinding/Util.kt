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
import kotlin.collections.set

// todo improve performance

internal fun <K, V> Component.getMultiBindingMap(mapName: Any): Map<K, Binding<V>> {
    val allMapBindings = getAllBindings()
        .mapNotNull { binding ->
            binding.attributes.get<Map<Any, MapBinding>>(KEY_MAP_BINDINGS)
                ?.get(mapName)?.let { binding to it }
        }

    val mapBindingsToUse = linkedMapOf<Any, Binding<*>>()

    allMapBindings.forEach { (binding, mapBinding) ->
        mapBindingsToUse[mapBinding.key] = binding
    }

    return mapBindingsToUse as Map<K, Binding<V>>
}

internal fun <V> Component.getMultiBindingSet(setName: Any): Set<Binding<V>> {
    val allSetBindings = getAllBindings()
        .mapNotNull { binding ->
            binding.attributes.get<Map<Any, SetBinding>>(KEY_SET_BINDINGS)
                ?.get(setName)?.let { binding to it }
        }

    val setBindingsToUse = linkedMapOf<Key, Binding<*>>()

    allSetBindings.forEach { (binding, setBinding) ->
        setBindingsToUse[binding.key] = binding
    }

    return setBindingsToUse.values.toSet() as Set<Binding<V>>
}

internal fun Component.getAllBindings(): Set<Binding<*>> =
    linkedSetOf<Binding<*>>().also { collectBindings(it) }

internal fun Component.collectBindings(
    bindings: MutableSet<Binding<*>>
) {
    dependencies.forEach { it.collectBindings(bindings) }
    bindings.addAll(this.bindings.values)
}