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

import com.ivianuu.injekt.*

/**
 * Returns a multi bound [Set] for [T] [qualifier] and passes [parameters] to any of the entries
 */
fun <T> Component.getSet(qualifier: Qualifier, parameters: ParametersDefinition? = null): Set<T> =
    get<MultiBindingSet<T>>(qualifier).toSet(parameters)

/**
 * Returns multi bound [Set] of [Lazy]s for [T] [qualifier] and passes [parameters] to any of the entries
 */
fun <T> Component.getLazySet(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Set<Lazy<T>> =
    get<MultiBindingSet<T>>(qualifier).toLazySet(parameters)

/**
 * Returns a multi bound [Set] of [Provider]s for [T] [qualifier] and passes [defaultParameters] to each [Provider]
 */
fun <T> Component.getProviderSet(
    qualifier: Qualifier,
    defaultParameters: ParametersDefinition? = null
): Set<Provider<T>> = get<MultiBindingSet<T>>(qualifier).toProviderSet(defaultParameters)

/**
 * Lazily Returns a multi bound [Set] for [T] [qualifier] and passes [parameters] to any of the entries
 */
fun <T> Component.injectSet(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Lazy<Set<T>> = lazy { getSet<T>(qualifier, parameters) }

/**
 * LazilyReturns multi bound [Set] of [Lazy]s for [T] [qualifier] and passes [parameters] to any of the entries
 */
fun <T> Component.injectLazySet(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Lazy<Set<Lazy<T>>> =
    lazy { getLazySet<T>(qualifier, parameters) }

/**
 * Lazily Returns a multi bound [Set] of [Provider]s for [T] [qualifier] and passes [defaultParameters] to each [Provider]
 */
fun <T> Component.injectProviderSet(
    qualifier: Qualifier,
    defaultParameters: ParametersDefinition? = null
): Lazy<Set<Provider<T>>> =
    lazy { getProviderSet<T>(qualifier, defaultParameters) }

/** Calls trough [Component.getSet] */
fun <T> InjektTrait.getSet(qualifier: Qualifier, parameters: ParametersDefinition? = null): Set<T> =
    component.getSet(qualifier, parameters)

/** Calls trough [Component.getLazySet] */
fun <T> InjektTrait.getLazySet(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Set<Lazy<T>> =
    component.getLazySet(qualifier, parameters)

/** Calls trough [Component.getProviderSet] */
fun <T> InjektTrait.getProviderSet(
    qualifier: Qualifier,
    defaultParameters: ParametersDefinition? = null
): Set<Provider<T>> =
    component.getProviderSet(qualifier, defaultParameters)

/** Calls trough [Component.injectSet] */
fun <T> InjektTrait.injectSet(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Lazy<Set<T>> =
    lazy { component.getSet<T>(qualifier, parameters) }

/** Calls trough [Component.injectLazySet] */
fun <T> InjektTrait.injectLazySet(
    qualifier: Qualifier,
    parameters: ParametersDefinition? = null
): Lazy<Set<Lazy<T>>> =
    lazy { component.getLazySet<T>(qualifier, parameters) }

/** Calls trough [Component.injectProviderSet] */
fun <T> InjektTrait.injectProviderSet(
    qualifier: Qualifier,
    defaultParameters: ParametersDefinition? = null
): Lazy<Set<Provider<T>>> =
    lazy { component.getProviderSet<T>(qualifier, defaultParameters) }