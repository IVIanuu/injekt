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