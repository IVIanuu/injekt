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

import com.ivianuu.injekt.BindingBuilder
import com.ivianuu.injekt.getOrSet

/**
 * Adds this binding into a map
 */
fun <T : V, K, V> BindingBuilder<T>.bindIntoMap(mapBinding: MapBinding<K, V>) {
    attributes.getOrSet(KEY_MAP_BINDINGS) { hashMapOf<Any, MapBinding<K, V>>() }
        .put(mapBinding.mapName, mapBinding)
}

/**
 * Adds this binding into a map
 */
fun <T : V, K, V> BindingBuilder<T>.bindIntoMap(
    mapName: MapName<K, V>,
    key: K
) {
    bindIntoMap(MapBinding(mapName, key))
}