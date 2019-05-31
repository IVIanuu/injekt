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

package com.ivianuu.injekt.provider

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import kotlin.reflect.KClass

/** Calls trough [Component.getProvider] */
inline fun <reified T> InjektTrait.getProvider(
    name: Qualifier? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Provider<T> = getProvider(T::class, name, defaultParameters)

/** Calls trough [Component.getProvider] */
fun <T> InjektTrait.getProvider(
    type: KClass<*>,
    name: Qualifier? = null,
    defaultParameters: ParametersDefinition? = null
): Provider<T> = component.getProvider(type, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
inline fun <reified T> InjektTrait.injectProvider(
    name: Qualifier? = null,
    noinline defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> = injectProvider(T::class, name, defaultParameters)

/** Calls trough [Component.injectProvider] */
fun <T> InjektTrait.injectProvider(
    type: KClass<*>,
    name: Qualifier? = null,
    defaultParameters: ParametersDefinition? = null
): Lazy<Provider<T>> =
    lazy(LazyThreadSafetyMode.NONE) { component.getProvider<T>(type, name, defaultParameters) }