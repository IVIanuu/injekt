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
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.getOrSet
import com.ivianuu.injekt.withBinding
import kotlin.collections.set
import kotlin.reflect.KClass

/**
 * Binds this binding into a map
 */
infix fun <T> BindingContext<T>.bindIntoMap(mapBinding: MapBinding): BindingContext<T> {
    binding.attributes.getOrSet(KEY_MAP_BINDINGS) {
        hashMapOf<String, MapBinding>()
    }[mapBinding.mapName] = mapBinding

    module.declareMapBinding(mapBinding.mapName)

    return this
}

/**
 * Binds this binding into the name [Pair.first] with the key [Pair.second]
 */
infix fun <T> BindingContext<T>.bindIntoMap(
    pair: Pair<String, Any>
): BindingContext<T> = bindIntoMap(MapBinding(pair.first, pair.second))

/**
 * Binds this binding into [mapName] with [mapKey]
 */
fun <T> BindingContext<T>.bindIntoMap(
    mapName: String,
    mapKey: Any,
    override: Boolean = false
): BindingContext<T> = bindIntoMap(MapBinding(mapName, mapKey, override))

/**
 * Declares a empty map binding
 * This is useful for retrieving a [MultiBindingMap] even if no [Binding] was bound into it
 */
fun Module.mapBinding(mapName: String) {
    factory(name = mapName, override = true) {
        MultiBindingMap<Any, Any>(component, emptyMap())
    }
}

/**
 * Binds a already existing [Binding] into [mapBinding]
 */
inline fun <reified T> Module.bindIntoMap(
    mapBinding: MapBinding,
    implementationName: String? = null
) {
    bindIntoMap<T>(mapBinding, T::class, implementationName)
}

/**
 * Binds a already existing [Binding] into [mapName] with [mapKey]
 */
inline fun <reified T> Module.bindIntoMap(
    mapName: String,
    mapKey: Any,
    override: Boolean = false,
    implementationName: String? = null
) {
    bindIntoMap<T>(MapBinding(mapName, mapKey, override), implementationName)
}

/**
 * Binds a already existing [Binding] into [mapBinding]
 */
fun <T> Module.bindIntoMap(
    mapBinding: MapBinding,
    implementationType: KClass<*>,
    implementationName: String? = null
) {
    withBinding<T>(implementationType, implementationName) {
        bindIntoMap(mapBinding)
        binding.attributes[KEY_ORIGINAL_KEY] = Key(implementationType, implementationName)
    }
}

/**
 * Binds a already existing [Binding] into [mapName] with [mapKey]
 */
fun <T> Module.bindIntoMap(
    mapName: String,
    mapKey: Any,
    override: Boolean = false,
    implementationType: KClass<*>,
    implementationName: String? = null
) {
    bindIntoMap<T>(MapBinding(mapName, mapKey, override), implementationType, implementationName)
}