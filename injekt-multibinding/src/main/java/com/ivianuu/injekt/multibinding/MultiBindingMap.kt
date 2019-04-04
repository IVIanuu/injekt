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
 * Wraps a [Map] of [Binding]s
 */
data class MultiBindingMap<K, T>(
    val component: Component,
    val map: Map<K, Binding<T>>
)

/**
 * Returns a [Map] of [K] and [T]s
 */
fun <K, T> MultiBindingMap<K, T>.toMap(parameters: ParametersDefinition? = null): Map<K, T> =
    map.mapValues { component.get<T>(it.value.type, it.value.qualifier, parameters) }

/**
 * Returns a [Map] of [K] and [Lazy]s of [T]
 */
fun <K, T> MultiBindingMap<K, T>.toLazyMap(parameters: ParametersDefinition? = null): Map<K, Lazy<T>> =
    map.mapValues { lazy { component.get<T>(it.value.type, it.value.qualifier, parameters) } }

/**
 * Returns a [Map] of [K] and [Provider]s of [T]
 */
fun <K, T> MultiBindingMap<K, T>.toProviderMap(defaultParameters: ParametersDefinition? = null): Map<K, Provider<T>> =
    map.mapValues { binding ->
        provider {
            component.get<T>(
                binding.value.type,
                binding.value.qualifier,
                it ?: defaultParameters
            )
        }
    }