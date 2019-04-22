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
 * Returns a multi bound [Map] for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K, V> Component.getMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Map<K, V> = getMultiBindingMap(name).mapValues {
    get<V>(it.value.type, it.value.name, parameters)
}

/**
 * Returns multi bound [Map] of [Lazy]s for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K, V> Component.getLazyMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Map<K, Lazy<V>> {
    return getMultiBindingMap(name).mapValues {
        lazy { get<V>(it.value.type, it.value.name, parameters) }
    }
}

/**
 * Returns a multi bound [Map] of [Provider]s for [K], [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <K, V> Component.getProviderMap(
    name: MapName<K, V>,
    defaultParameters: ParametersDefinition? = null
): Map<K, Provider<V>> {
    return getMultiBindingMap(name).mapValues { (_, binding) ->
        provider {
            get<V>(
                binding.type,
                binding.name,
                it ?: defaultParameters
            )
        }
    }
}
/**
 * Lazily Returns a multi bound [Map] for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K, V> Component.injectMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, V>> {
    return lazy { getMap(name, parameters) }
}

/**
 * LazilyReturns multi bound [Map] of [Lazy]s for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K, V> Component.injectLazyMap(
    name: MapName<K, V>,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, Lazy<V>>> =
    lazy { getLazyMap(name, parameters) }

/**
 * Lazily Returns a multi bound [Map] of [Provider]s for [K], [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <K, V> Component.injectProviderMap(
    name: MapName<K, V>,
    defaultParameters: ParametersDefinition? = null
): Lazy<Map<K, Provider<V>>> =
    lazy { getProviderMap(name, defaultParameters) }