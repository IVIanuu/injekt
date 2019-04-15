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
 * Returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.getSet(name: Name, parameters: ParametersDefinition? = null): Set<T> =
    getMultiBindingSet<T>(name).map { get<T>(it.type, it.name, parameters) }.toSet()

/**
 * Returns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.getLazySet(
    name: Name,
    parameters: ParametersDefinition? = null
): Set<Lazy<T>> =
    getMultiBindingSet<T>(name).map {
        lazy { get<T>(it.type, it.name, parameters) }
    }.toSet()

/**
 * Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T> Component.getProviderSet(
    name: Name,
    defaultParameters: ParametersDefinition? = null
): Set<Provider<T>> = getMultiBindingSet<T>(name).map { binding ->
    provider {
        get<T>(
            binding.type,
            binding.name,
            it ?: defaultParameters
        )
    }
}.toSet()

/**
 * Lazily Returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.injectSet(
    name: Name,
    parameters: ParametersDefinition? = null
): Lazy<Set<T>> = lazy(LazyThreadSafetyMode.NONE) { getSet<T>(name, parameters) }

/**
 * LazilyReturns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.injectLazySet(
    name: Name,
    parameters: ParametersDefinition? = null
): Lazy<Set<Lazy<T>>> =
    lazy { getLazySet<T>(name, parameters) }

/**
 * Lazily Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T> Component.injectProviderSet(
    name: Name,
    defaultParameters: ParametersDefinition? = null
): Lazy<Set<Provider<T>>> =
    lazy { getProviderSet<T>(name, defaultParameters) }