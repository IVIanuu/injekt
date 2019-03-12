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

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.get

/**
 * Returns a multi bound [Map] for [K], [T] [qualifier] and passes [parameters] to any of the entries
 */
fun <K, T> Component.getMap(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Map<K, T> = get<MultiBindingMap<K, T>>(qualifier).toMap(parameters)

/**
 * Returns multi bound [Map] of [Lazy]s for [K], [T] [qualifier] and passes [parameters] to any of the entries
 */
fun <K, T> Component.getLazyMap(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Map<K, Lazy<T>> =
    get<MultiBindingMap<K, T>>(qualifier).toLazyMap(parameters)

/**
 * Returns a multi bound [Map] of [Provider]s for [K], [T] [qualifier] and passes [defaultParameters] to each [Provider]
 */
fun <K, T> Component.getProviderMap(
    qualifier: Qualifier,
    defaultParameters: ParametersDefinition? = null
): Map<K, Provider<T>> =
    get<MultiBindingMap<K, T>>(qualifier).toProviderMap(defaultParameters)

/**
 * Lazily Returns a multi bound [Map] for [K], [T] [qualifier] and passes [parameters] to any of the entries
 */
fun <K, T> Component.injectMap(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, T>> =
    lazy { getMap<K, T>(qualifier, parameters) }

/**
 * LazilyReturns multi bound [Map] of [Lazy]s for [K], [T] [qualifier] and passes [parameters] to any of the entries
 */
fun <K, T> Component.injectLazyMap(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, Lazy<T>>> =
    lazy { getLazyMap<K, T>(qualifier, parameters) }

/**
 * Lazily Returns a multi bound [Map] of [Provider]s for [K], [T] [qualifier] and passes [defaultParameters] to each [Provider]
 */
fun <K, T> Component.injectProviderMap(
    qualifier: Qualifier,
    defaultParameters: ParametersDefinition? = null
): Lazy<Map<K, Provider<T>>> =
    lazy { getProviderMap<K, T>(qualifier, defaultParameters) }

/** Calls trough [Component.getMap] */
fun <K, T> InjektTrait.getMap(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Map<K, T> =
    component.getMap(qualifier, parameters)

/** Calls trough [Component.getLazyMap] */
fun <K, T> InjektTrait.getLazyMap(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Map<K, Lazy<T>> =
    component.getLazyMap(qualifier, parameters)

/** Calls trough [Component.getProviderMap] */
fun <K, T> InjektTrait.getProviderMap(
    qualifier: Qualifier,
    defaultParameters: ParametersDefinition? = null
): Map<K, Provider<T>> =
    component.getProviderMap(qualifier, defaultParameters)

/** Calls trough [Component.injectMap] */
fun <K, T> InjektTrait.injectMap(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, T>> =
    lazy { component.getMap<K, T>(qualifier, parameters) }

/** Calls trough [Component.injectLazyMap] */
fun <K, T> InjektTrait.injectLazyMap(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, Lazy<T>>> =
    lazy { component.getLazyMap<K, T>(qualifier, parameters) }

/** Calls trough [Component.injectProviderMap] */
fun <K, T> InjektTrait.injectProviderMap(
    qualifier: Qualifier,
    defaultParameters: ParametersDefinition? = null
): Lazy<Map<K, Provider<T>>> =
    lazy { component.getProviderMap<K, T>(qualifier, defaultParameters) }