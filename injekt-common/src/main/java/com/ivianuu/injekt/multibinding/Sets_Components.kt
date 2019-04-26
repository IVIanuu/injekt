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
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.provider.Provider
import com.ivianuu.injekt.provider.provider

/**
 * Returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.getSet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Set<T> {
    return getMultiBindingSet(name)
        .map { get<T>(it.type, it.name, parameters) }
        .toSet()
}

/**
 * Returns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.getLazySet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Set<Lazy<T>> {
    return getMultiBindingSet(name).map {
        lazy { get<T>(it.type, it.name, parameters) }
    }.toSet()
}

/**
 * Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T> Component.getProviderSet(
    name: SetName<T>,
    defaultParameters: ParametersDefinition? = null
): Set<Provider<T>> {
    return getMultiBindingSet(name).map { binding ->
        provider {
            get<T>(
                binding.type,
                binding.name,
                it ?: defaultParameters
            )
        }
    }.toSet()
}

/**
 * Lazily returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.injectSet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Lazy<Set<T>> = lazy(LazyThreadSafetyMode.NONE) { getSet(name, parameters) }

/**
 * Lazily returns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T> Component.injectLazySet(
    name: SetName<T>,
    parameters: ParametersDefinition? = null
): Lazy<Set<Lazy<T>>> =
    lazy { getLazySet(name, parameters) }

/**
 * Lazily returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T> Component.injectProviderSet(
    name: SetName<T>,
    defaultParameters: ParametersDefinition? = null
): Lazy<Set<Provider<T>>> =
    lazy { getProviderSet(name, defaultParameters) }