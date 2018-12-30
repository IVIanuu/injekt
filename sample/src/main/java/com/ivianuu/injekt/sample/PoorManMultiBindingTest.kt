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

package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Declaration
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.getOrSet
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.collections.emptyMap
import kotlin.collections.emptySet
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.mapKeys
import kotlin.collections.mapNotNull
import kotlin.collections.mapValues
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.set
import kotlin.collections.toMap
import kotlin.collections.toSet

const val KEY_MAP_BINDINGS = "mapBindings"
const val KEY_SET_BINDINGS = "setBindings"

inline fun <reified K : Any, reified T : Any> Module.declareMapBinding(mapName: String) {
    factory(name = mapName, override = true) { emptyMap<K, T>() }
}

inline infix fun <reified K : Any, V : Any, reified T : V> Declaration<T>.intoMap(pair: Pair<String, K>) =
    apply {
        val (mapName, mapKey) = pair

        attributes.getOrSet(KEY_MAP_BINDINGS) { mutableMapOf<String, Any>() }[mapName] = mapKey

        module.factory(name = mapName, override = true) { params ->
            instance.component.declarationRegistry
                .getAllDeclarations()
                .mapNotNull { declaration ->
                    declaration.attributes.get<Map<String, Any>>(KEY_MAP_BINDINGS)
                        ?.get(mapName)?.let { it to declaration }
                }
                .toMap()
                .mapKeys { it.key as K }
                .mapValues { it.value.resolveInstance(null) as V }
        }
    }

inline fun <reified T : Any> Module.declareSetBinding(setName: String) {
    factory(name = setName, override = true) { emptySet<T>() }
}

inline infix fun <T : Any, reified S : T> Declaration<S>.intoSet(setName: String) = apply {
    attributes.getOrSet(KEY_SET_BINDINGS) { mutableSetOf<String>() }.add(setName)

    module.factory(name = setName, override = true) { params ->
        instance.component.declarationRegistry
            .getAllDeclarations()
            .filter { it.attributes.get<Set<String>>(KEY_SET_BINDINGS)?.contains(setName) == true }
            .map { it.resolveInstance(null) }
            .toSet()
    }
}