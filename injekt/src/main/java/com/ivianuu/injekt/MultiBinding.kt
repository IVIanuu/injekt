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

data class MultiBindingSet<T : Any>(val set: Set<Declaration<T>>)

fun <T : Any> MultiBindingSet<T>.toSet(params: ParamsDefinition? = null): Set<T> =
    set.map { it.resolveInstance(params) }.toSet()

fun <T : Any> MultiBindingSet<T>.toProviderSet(defaultParams: ParamsDefinition? = null): Set<Provider<T>> =
    set.map { DeclarationProvider(it, defaultParams) }.toSet()

fun <T : Any> MultiBindingSet<T>.toLazySet(params: ParamsDefinition? = null): Set<Lazy<T>> =
    set.map { lazy { it.resolveInstance(params) } }.toSet()

data class MultiBindingMap<K : Any, T : Any>(val map: Map<K, Declaration<T>>)

fun <K : Any, T : Any> MultiBindingMap<K, T>.toMap(params: ParamsDefinition? = null) =
    map.mapValues { it.value.resolveInstance(params) }

fun <K : Any, T : Any> MultiBindingMap<K, T>.toProviderMap(defaultParams: ParamsDefinition? = null) =
    map.mapValues { DeclarationProvider(it.value, defaultParams) }

fun <K : Any, T : Any> MultiBindingMap<K, T>.toLazyMap(params: ParamsDefinition? = null) =
    map.mapValues { lazy { it.value.resolveInstance(params) } }