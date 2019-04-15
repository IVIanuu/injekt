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

package com.ivianuu.injekt

/**
 * Holds a [Component] and allows for shorter syntax
 */
interface InjektTrait {
    /**
     * The [Component] of this class
     */
    val component: Component
}

/** Calls trough [Component.get] */
inline fun <reified T> InjektTrait.get(
    name: Name? = null,
    noinline parameters: ParametersDefinition? = null
): T = component.get(name, parameters)

/** Calls trough [Component.inject] */
inline fun <reified T> InjektTrait.inject(
    name: Name? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { component.get<T>(name, parameters) }

/** Calls trough [Component.getProvider] */
inline fun <reified T> InjektTrait.getProvider(
    name: Name? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = component.getProvider(name, defaultParameters)

/** Calls trough [Component.injectProvider] */
inline fun <reified T> InjektTrait.injectProvider(
    name: Name? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> =
    lazy(LazyThreadSafetyMode.NONE) { component.getProvider<T>(name, defaultParameters) }