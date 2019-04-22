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

internal fun <K, V> Component.getMultiBindingMap(mapName: MapName<K, V>): Map<K, Binding<V>> {
    return getAllBindings()
        .mapNotNull { binding ->
            binding.attributes.get<Map<MapName<*, *>, MapBinding<*, *>>>(KEY_MAP_BINDINGS)
                ?.get(mapName)
                ?.let { it.key to binding }
        }
        .toMap() as Map<K, Binding<V>>
}

internal fun <T> Component.getMultiBindingSet(setName: SetName<T>): Set<Binding<T>> {
    return getAllBindings()
        .filter {
            it.attributes.get<Map<SetName<*>, SetBinding<*>>>(KEY_SET_BINDINGS)
                ?.get(setName) != null
        }
        .toSet() as Set<Binding<T>>
}

internal fun Component.getAllBindings(): List<Binding<*>> =
    arrayListOf<Binding<*>>().also { collectBindings(it) }

internal fun Component.collectBindings(
    bindings: MutableList<Binding<*>>
) {
    dependencies.forEach { it.collectBindings(bindings) }
    bindings.addAll(this.bindings.values)
}