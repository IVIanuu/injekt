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

import com.ivianuu.injekt.BindingContext

import com.ivianuu.injekt.getOrSet
import kotlin.collections.set

/**
 * Adds this binding into a map
 */
infix fun <T> BindingContext<T>.bindIntoMap(mapBinding: MapBinding): BindingContext<T> {
    binding.attributes.getOrSet(KEY_MAP_BINDINGS) {
        linkedMapOf<Any, MapBinding>()
    }[mapBinding.mapName] = mapBinding
    return this
}

/**
 * Adds this binding into the name [Pair.first] with the key [Pair.second]
 */
infix fun <T> BindingContext<T>.bindIntoMap(
    pair: Pair<Any, Any>
): BindingContext<T> = bindIntoMap(MapBinding(pair.first, pair.second))

/**
 * Adds this binding into [mapName] with [mapKey]
 */
fun <T> BindingContext<T>.bindIntoMap(
    mapName: Any,
    mapKey: Any
): BindingContext<T> = bindIntoMap(MapBinding(mapName, mapKey))