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
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(T::class, name, parameters)

/** Calls trough [Component.get] */
fun <T> InjektTrait.get(
    type: KClass<*>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): T = component.get(type, name, parameters)

/** Calls trough [Component.inject] */
inline fun <reified T> InjektTrait.inject(
    name: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = inject(T::class, name, parameters)

/** Calls trough [Component.inject] */
fun <T> InjektTrait.inject(
    type: KClass<*>,
    name: Qualifier? = null,
    parameters: ParametersDefinition? = null
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { component.get<T>(type, name, parameters) }