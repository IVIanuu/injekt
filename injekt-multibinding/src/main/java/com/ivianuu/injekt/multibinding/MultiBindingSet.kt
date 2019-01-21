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

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.provider

/**
 * Wraps a [Set] of [Binding]s
 */
data class MultiBindingSet<T>(
    val component: Component,
    val set: Set<Binding<T>>
)

/**
 * Returns a [Set] of [T]s
 */
fun <T> MultiBindingSet<T>.toSet(parameters: ParametersDefinition? = null): Set<T> =
    set.map {
        component.get<T>(it.type, it.name, parameters = parameters)
    }.toSet()

/**
 * Returns a [Set] of [Lazy]s of [T]
 */
fun <T> MultiBindingSet<T>.toLazySet(parameters: ParametersDefinition? = null): Set<Lazy<T>> =
    set.map { lazy { component.get<T>(it.type, it.name, parameters = parameters) } }.toSet()

/**
 * Returns a [Set] of [Provider]s of [T]
 */
fun <T> MultiBindingSet<T>.toProviderSet(defaultParameters: ParametersDefinition? = null): Set<Provider<T>> =
    set.map { (_, type, name) ->
        provider {
            component.get<T>(
                type,
                name,
                it ?: defaultParameters
            )
        }
    }.toSet()