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

package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Describes a set multi binding
 */
data class SetBinding(
    val type: KClass<*>,
    val name: String
)

/**
 * Describes a map multi binding
 */
data class MapBinding(
    val keyType: KClass<*>,
    val type: KClass<*>,
    val name: String
)

/**
 * Wraps a [Set] of [Declaration]s
 */
data class MultiBindingSet<T : Any>(val set: Set<Declaration<T>>)

/**
 * Returns a [Set] of [T]s
 */
fun <T : Any> MultiBindingSet<T>.toSet(params: ParamsDefinition? = null): Set<T> =
    set.map { it.resolveInstance(params) }.toSet()

/**
 * Returns a [Set] of [Provider]s of [T]
 */
fun <T : Any> MultiBindingSet<T>.toProviderSet(defaultParams: ParamsDefinition? = null): Set<Provider<T>> =
    set.map { DeclarationProvider(it, defaultParams) }.toSet()

/**
 * Returns a [Set] of [Lazy]s of [T]
 */
fun <T : Any> MultiBindingSet<T>.toLazySet(params: ParamsDefinition? = null): Set<Lazy<T>> =
    set.map { lazy { it.resolveInstance(params) } }.toSet()

/**
 * Wraps a [Map] of [Declaration]s
 */
data class MultiBindingMap<K : Any, T : Any>(val map: Map<K, Declaration<T>>)

/**
 * Returns a [Map] of [K] and [T]s
 */
fun <K : Any, T : Any> MultiBindingMap<K, T>.toMap(params: ParamsDefinition? = null) =
    map.mapValues { it.value.resolveInstance(params) }

/**
 * Returns a [Map] of [K] and [Provider]s of [T]
 */
fun <K : Any, T : Any> MultiBindingMap<K, T>.toProviderMap(defaultParams: ParamsDefinition? = null) =
    map.mapValues { DeclarationProvider(it.value, defaultParams) }

/**
 * Returns a [Map] of [K] and [Lazy]s of [T]
 */
fun <K : Any, T : Any> MultiBindingMap<K, T>.toLazyMap(params: ParamsDefinition? = null) =
    map.mapValues { lazy { it.value.resolveInstance(params) } }