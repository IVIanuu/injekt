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

/** Calls trough [Component.getSet] */
fun <T> InjektTrait.getSet(name: Name, parameters: ParametersDefinition? = null): Set<T> =
    component.getSet(name, parameters)

/** Calls trough [Component.getLazySet] */
fun <T> InjektTrait.getLazySet(
    name: Name,
    parameters: ParametersDefinition? = null
): Set<Lazy<T>> =
    component.getLazySet(name, parameters)

/** Calls trough [Component.getProviderSet] */
fun <T> InjektTrait.getProviderSet(
    name: Name,
    defaultParameters: ParametersDefinition? = null
): Set<Provider<T>> =
    component.getProviderSet(name, defaultParameters)

/** Calls trough [Component.injectSet] */
fun <T> InjektTrait.injectSet(
    name: Name,
    parameters: ParametersDefinition? = null
): Lazy<Set<T>> =
    lazy { component.getSet<T>(name, parameters) }

/** Calls trough [Component.injectLazySet] */
fun <T> InjektTrait.injectLazySet(
    name: Name,
    parameters: ParametersDefinition? = null
): Lazy<Set<Lazy<T>>> =
    lazy { component.getLazySet<T>(name, parameters) }

/** Calls trough [Component.injectProviderSet] */
fun <T> InjektTrait.injectProviderSet(
    name: Name,
    defaultParameters: ParametersDefinition? = null
): Lazy<Set<Provider<T>>> =
    lazy { component.getProviderSet<T>(name, defaultParameters) }