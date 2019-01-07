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

import com.ivianuu.injekt.BeanDefinition
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.getProvider

/**
 * Wraps a [Set] of [BeanDefinition]s
 */
data class MultiBindingSet<T : Any>(val set: Set<BeanDefinition<T>>)

/**
 * Returns a [Set] of [T]s
 */
fun <T : Any> MultiBindingSet<T>.toSet(parameters: ParametersDefinition? = null): Set<T> =
    set.map { it.resolveInstance(parameters = parameters) }.toSet()

/**
 * Returns a [Set] of [Lazy]s of [T]
 */
fun <T : Any> MultiBindingSet<T>.toLazySet(parameters: ParametersDefinition? = null): Set<Lazy<T>> =
    set.map { lazy { it.resolveInstance(parameters = parameters) } }.toSet()

/**
 * Returns a [Set] of [Provider]s of [T]
 */
fun <T : Any> MultiBindingSet<T>.toProviderSet(defaultParameters: ParametersDefinition? = null): Set<Provider<T>> =
    set.map { dec -> getProvider { dec.resolveInstance(it ?: defaultParameters) } }.toSet()