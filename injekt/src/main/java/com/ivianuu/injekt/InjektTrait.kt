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
 * Holds a [Component] and allows for shorter syntax
 */
interface InjektTrait {
    /**
     * The [Component] of this class
     */
    val component: Component
}

/** Calls trough [Component.get] */
inline fun <reified T> InjektTrait.get(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(T::class, name, parameters)

/** Calls trough [Component.get] */
fun <T> InjektTrait.get(
    type: KClass<*>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): T = component.get(type, name, parameters)

/** Calls trough [Component.get] */
inline fun <reified T> InjektTrait.getOrNull(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T? = getOrNull(T::class, name, parameters)

/** Calls trough [Component.get] */
fun <T> InjektTrait.getOrNull(
    type: KClass<*>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): T? = component.getOrNull(type, name, parameters)

/** Calls trough [Component.inject] */
inline fun <reified T> InjektTrait.inject(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(T::class, name, parameters)

/** Calls trough [Component.inject] */
fun <T> InjektTrait.inject(
    type: KClass<*>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { component.get<T>(type, name, parameters) }

/** Calls trough [Component.injectOrNull] */
inline fun <reified T> InjektTrait.injectOrNull(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T?> = injectOrNull(T::class, name, parameters)

/** Calls trough [Component.injectOrNull] */
fun <T> InjektTrait.injectOrNull(
    type: KClass<*>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): Lazy<T?> = lazy(LazyThreadSafetyMode.NONE) { component.getOrNull<T>(type, name, parameters) }

/** Calls trough [Component.getProvider] */
inline fun <reified T> InjektTrait.getProvider(
    name: Qualifier? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = getProvider(T::class, name, defaultParameters)

/** Calls trough [Component.getProvider] */
fun <T> InjektTrait.getProvider(
    type: KClass<*>,
    name: Qualifier? = null,
    defaultParameters: ParametersDefinition? = null
): Provider<T> = component.getProvider(type, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
inline fun <reified T> InjektTrait.injectProvider(
    name: Qualifier? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = injectProvider(T::class, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
fun <T> InjektTrait.injectProvider(
    type: KClass<*>,
    name: Qualifier? = null,
    defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> =
    lazy(LazyThreadSafetyMode.NONE) { component.getProvider<T>(type, name, defaultParameters) }

/** Calls trough [Component.getMap] */
fun <K, V> InjektTrait.getMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Map<K, V> =
    component.getMap(name, parameters)

/** Calls trough [Component.getLazyMap] */
fun <K, V> InjektTrait.getLazyMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Map<K, Lazy<V>> =
    component.getLazyMap(name, parameters)

/** Calls trough [Component.getProviderMap] */
fun <K, V> InjektTrait.getProviderMap(
    name: MapName<K, V>,
    defaultParameters: ParametersDefinition? = null
): Map<K, Provider<V>> =
    component.getProviderMap(name, defaultParameters)

/** Calls trough [Component.injectMap] */
fun <K, V> InjektTrait.injectMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, V>> =
    lazy { component.getMap<K, V>(name, parameters) }

/** Calls trough [Component.injectLazyMap] */
fun <K, V> InjektTrait.injectLazyMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, Lazy<V>>> =
    lazy { component.getLazyMap<K, V>(name, parameters) }

/** Calls trough [Component.injectProviderMap] */
fun <K, V> InjektTrait.injectProviderMap(
    name: MapName<K, V>,
    defaultParameters: ParametersDefinition? = null
): Lazy<Map<K, Provider<V>>> =
    lazy { component.getProviderMap<K, V>(name, defaultParameters) }

/** Calls trough [Component.getSet] */
fun <T> InjektTrait.getSet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Set<T> = component.getSet(name, parameters)

/** Calls trough [Component.getLazySet] */
fun <T> InjektTrait.getLazySet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Set<Lazy<T>> =
    component.getLazySet(name, parameters)

/** Calls trough [Component.getProviderSet] */
fun <T> InjektTrait.getProviderSet(
    name: SetName<T>,
    defaultParameters: ParametersDefinition? = null
): Set<Provider<T>> =
    component.getProviderSet(name, defaultParameters)

/** Calls trough [Component.injectSet] */
fun <T> InjektTrait.injectSet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Lazy<Set<T>> =
    lazy { component.getSet(name, parameters) }

/** Calls trough [Component.injectLazySet] */
fun <T> InjektTrait.injectLazySet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Lazy<Set<Lazy<T>>> =
    lazy { component.getLazySet(name, parameters) }

/** Calls trough [Component.injectProviderSet] */
fun <T> InjektTrait.injectProviderSet(
    name: SetName<T>,
    defaultParameters: ParametersDefinition? = null
): Lazy<Set<Provider<T>>> =
    lazy { component.getProviderSet(name, defaultParameters) }