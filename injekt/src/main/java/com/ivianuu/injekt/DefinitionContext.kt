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

import kotlin.reflect.KClass

/**
 * Environment for [Definition]s
 */
class DefinitionContext(val component: Component)

/** Calls trough [Component.get] */
inline fun <reified T : Any> DefinitionContext.get(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(T::class, name, parameters)

/** Calls trough [Component.get] */
fun <T : Any> DefinitionContext.get(
    type: KClass<T>,
    name: String? = null,
    parameters: ParametersDefinition? = null
): T = component.get(type, name, parameters)

/** Calls trough [Component.inject] */
inline fun <reified T : Any> DefinitionContext.inject(
    name: String? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(T::class, name, parameters)

/** Calls trough [Component.inject] */
fun <T : Any> DefinitionContext.inject(
    type: KClass<T>,
    name: String? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy { get(type, name, parameters) }

/** Calls trough [Component.provider] */
inline fun <reified T : Any> DefinitionContext.provider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = provider(T::class, name, defaultParameters)

/** Calls trough [Component.provider] */
fun <T : Any> DefinitionContext.provider(
    type: KClass<T>,
    name: String? = null,
    defaultParameters: ParametersDefinition? = null
): Provider<T> = component.provider(type, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
inline fun <reified T : Any> DefinitionContext.lazyProvider(
    name: String? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = lazyProvider(T::class, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
fun <T : Any> DefinitionContext.lazyProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = component.injectProvider(type, name, defaultParameters)