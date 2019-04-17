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
import com.ivianuu.injekt.provider.Provider

/** Calls trough [Component.getMap] */
fun <K, T> InjektTrait.getMap(
    name: Any,
    parameters: ParametersDefinition? = null
): Map<K, T> =
    component.getMap(name, parameters)

/** Calls trough [Component.getLazyMap] */
fun <K, T> InjektTrait.getLazyMap(
    name: Any,
    parameters: ParametersDefinition? = null
): Map<K, Lazy<T>> =
    component.getLazyMap(name, parameters)

/** Calls trough [Component.getProviderMap] */
fun <K, T> InjektTrait.getProviderMap(
    name: Any,
    defaultParameters: ParametersDefinition? = null
): Map<K, Provider<T>> =
    component.getProviderMap(name, defaultParameters)

/** Calls trough [Component.injectMap] */
fun <K, T> InjektTrait.injectMap(
    name: Any,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, T>> =
    lazy { component.getMap<K, T>(name, parameters) }

/** Calls trough [Component.injectLazyMap] */
fun <K, T> InjektTrait.injectLazyMap(
    name: Any,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, Lazy<T>>> =
    lazy { component.getLazyMap<K, T>(name, parameters) }

/** Calls trough [Component.injectProviderMap] */
fun <K, T> InjektTrait.injectProviderMap(
    name: Any,
    defaultParameters: ParametersDefinition? = null
): Lazy<Map<K, Provider<T>>> =
    lazy { component.getProviderMap<K, T>(name, defaultParameters) }