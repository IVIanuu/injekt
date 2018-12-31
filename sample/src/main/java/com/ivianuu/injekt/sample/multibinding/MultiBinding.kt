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

package com.ivianuu.injekt.sample.multibinding

import com.ivianuu.injekt.*
import java.util.*
import kotlin.collections.set

const val KEY_MAP_BINDINGS = "mapBindings"
const val KEY_SET_BINDINGS = "setBindings"

/**
 * Adds a declaration for a map of [K, T] with the name [mapName]
 */
fun <K : Any, T : Any> Module.mapBinding(mapName: String) {
    factory(name = mapName, override = true) { emptyMap<K, T>() }
}

infix fun <K : Any, T : Any, S : T> Declaration<S>.intoMap(pair: Pair<String, K>) =
    apply {
        val (mapName, mapKey) = pair

        attributes.getOrSet(KEY_MAP_BINDINGS) { mutableMapOf<String, Any>() }[mapName] = mapKey

        module.factory(name = mapName, override = true) {
            component.declarationRegistry
                .getAllDeclarations()
                .mapNotNull { declaration ->
                    declaration.attributes.get<Map<String, Any>>(KEY_MAP_BINDINGS)
                        ?.get(mapName)?.let { it to declaration }
                }
                .toMap()
                .mapKeys { it.key as K }
                .mapValues { it.value as Declaration<T> }
                .let { MultiBindingMap(it) }
        }
    }

fun <T : Any> Module.setBinding(setName: String) {
    factory(name = setName, override = true) { emptySet<T>() }
}

inline fun <reified T : Any, reified S : T> Module.bindIntoSet(
    setName: String,
    declarationName: String? = null
) =
    factory<T>(UUID.randomUUID().toString()) { get<S>(declarationName) } intoSet setName

inline fun <K : Any, reified T : Any, reified S : T> Module.bindIntoMap(
    mapName: String,
    key: K,
    declarationName: String? = null
) =
    factory<T>(UUID.randomUUID().toString()) { get<S>(declarationName) } intoMap (mapName to key)

infix fun <T : Any, S : T> Declaration<S>.intoSet(setName: String) = apply {
    attributes.getOrSet(KEY_SET_BINDINGS) { mutableSetOf<String>() }.add(setName)

    module.factory(name = setName, override = true) {
        component.declarationRegistry
            .getAllDeclarations()
            .filter { it.attributes.get<Set<String>>(KEY_SET_BINDINGS)?.contains(setName) == true }
            .map { it as Declaration<T> }
            .toSet()
            .let { MultiBindingSet(it) }
    }
}

/**
 * Wraps a [Set] of [Declaration]s
 */
data class MultiBindingSet<T : Any>(val set: Set<Declaration<T>>)

/**
 * Returns a [Set] of [T]s
 */
fun <T : Any> MultiBindingSet<T>.toSet(params: ParamsDefinition? = null): Set<T> =
    set.map { it.resolveInstance(params = params) }.toSet()

/**
 * Returns a [Set] of [Provider]s of [T]
 */
fun <T : Any> MultiBindingSet<T>.toProviderSet(defaultParams: ParamsDefinition? = null): Set<Provider<T>> =
    set.map { dec -> provider { dec.resolveInstance(it ?: defaultParams) } }.toSet()

/**
 * Returns a [Set] of [Lazy]s of [T]
 */
fun <T : Any> MultiBindingSet<T>.toLazySet(params: ParamsDefinition? = null): Set<Lazy<T>> =
    set.map { lazy { it.resolveInstance(params = params) } }.toSet()

/**
 * Wraps a [Map] of [Declaration]s
 */
data class MultiBindingMap<K : Any, T : Any>(val map: Map<K, Declaration<T>>)

/**
 * Returns a [Map] of [K] and [T]s
 */
fun <K : Any, T : Any> MultiBindingMap<K, T>.toMap(params: ParamsDefinition? = null) =
    map.mapValues { it.value.resolveInstance(params = params) }

/**
 * Returns a [Map] of [K] and [Provider]s of [T]
 */
fun <K : Any, T : Any> MultiBindingMap<K, T>.toProviderMap(defaultParams: ParamsDefinition? = null) =
    map.mapValues { dec -> provider { dec.value.resolveInstance(it ?: defaultParams) } }

/**
 * Returns a [Map] of [K] and [Lazy]s of [T]
 */
fun <K : Any, T : Any> MultiBindingMap<K, T>.toLazyMap(params: ParamsDefinition? = null) =
    map.mapValues { lazy { it.value.resolveInstance(params = params) } }