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
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.getOrSet
import com.ivianuu.injekt.withBinding
import kotlin.collections.set

/**
 * Adds this binding into a map
 */
infix fun <T> BindingContext<T>.bindIntoMap(mapBinding: MapBinding): BindingContext<T> {
    binding.attributes.getOrSet(KEY_MAP_BINDINGS) {
        hashMapOf<Qualifier, MapBinding>()
    }[mapBinding.mapQualifier] = mapBinding

    module.declareMapBinding(mapBinding.mapQualifier)

    return this
}

/**
 * Adds this binding into the name [Pair.first] with the key [Pair.second]
 */
infix fun <T> BindingContext<T>.bindIntoMap(
    pair: Pair<Qualifier, Any>
): BindingContext<T> = bindIntoMap(MapBinding(pair.first, pair.second))

/**
 * Adds this binding into [mapQualifier] with [mapKey]
 */
fun <T> BindingContext<T>.bindIntoMap(
    mapQualifier: Qualifier,
    mapKey: Any,
    override: Boolean = false
): BindingContext<T> = bindIntoMap(MapBinding(mapQualifier, mapKey, override))

/**
 * Declares a empty map binding
 * This is useful for retrieving a [MultiBindingMap] even if no [Binding] was bound into it
 */
fun Module.mapBinding(mapQualifier: Qualifier) {
    factory(qualifier = mapQualifier, override = true) {
        MultiBindingMap<Any, Any>(component, emptyMap())
    }
}

/**
 * Binds a already existing [Binding] into [mapQualifier] with [mapKey]
 */
inline fun <reified T> Module.bindIntoMap(
    mapQualifier: Qualifier,
    mapKey: Any,
    override: Boolean = false,
    implementationQualifier: Qualifier? = null
) {
    bindIntoMap<T>(MapBinding(mapQualifier, mapKey, override), implementationQualifier)
}

/**
 * Binds a already existing [Binding] into [mapBinding]
 */
inline fun <reified T> Module.bindIntoMap(
    mapBinding: MapBinding,
    implementationQualifier: Qualifier? = null
) {
    withBinding<T>(implementationQualifier) {
        bindIntoMap(mapBinding)
        binding.attributes[KEY_ORIGINAL_KEY] = Key(T::class, implementationQualifier)
    }
}