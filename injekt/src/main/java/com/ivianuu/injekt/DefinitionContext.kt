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
    noinline params: ParamsDefinition? = null
) = get(T::class, name, params)

/** Calls trough [Component.get] */
fun <T : Any> DefinitionContext.get(
    type: KClass<T>,
    name: String? = null,
    params: ParamsDefinition? = null
) = component.get(type, name, params)

/** Calls trough [Component.inject] */
inline fun <reified T : Any> DefinitionContext.inject(
    name: String? = null,
    noinline params: ParamsDefinition? = null
) = inject(T::class, name, params)

/** Calls trough [Component.inject] */
fun <T : Any> DefinitionContext.inject(
    type: KClass<T>,
    name: String? = null,
    params: ParamsDefinition? = null
): Lazy<T> = kotlin.lazy { get(type, name, params) }

/** Calls trough [Component.provider] */
inline fun <reified T : Any> DefinitionContext.provider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = provider(T::class, name, defaultParams)

/** Calls trough [Component.provider] */
fun <T : Any> DefinitionContext.provider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = component.provider(type, name, defaultParams)

/** Calls trough [Component.injectProvider] */
inline fun <reified T : Any> DefinitionContext.lazyProvider(
    name: String? = null,
    noinline defaultParams: ParamsDefinition? = null
) = lazyProvider(T::class, name, defaultParams)

/** Calls trough [Component.injectProvider] */
fun <T : Any> DefinitionContext.lazyProvider(
    type: KClass<T>,
    name: String? = null,
    defaultParams: ParamsDefinition? = null
) = component.injectProvider(type, name, defaultParams)