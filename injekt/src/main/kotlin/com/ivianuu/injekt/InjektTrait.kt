/*
 * Copyright 2019 Manuel Wrage
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
 *
 * Example:
 *
 * ```
 * class MainActivity : Activity(), InjektTrait {
 *
 *     override val Component = Component { ... }
 *
 *     private val dep1: Dependency1 by inject()
 *     private val dep2: Dependency2 by inject()
 *
 * }
 * ```
 *
 */
interface InjektTrait {

    /**
     * The Component which will be used in the below functions
     */
    val component: Component

    /**
     * @see Component.get
     */
    fun <T> InjektTrait.get(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T = component.get(type, name, parameters)

    /**
     * @see Component.getOrNull
     */
    fun <T> InjektTrait.getOrNull(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): T? = component.getOrNull(type, name, parameters)

    /**
     * @see Component.inject
     */
    fun <T> InjektTrait.inject(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): kotlin.Lazy<T> = lazy(LazyThreadSafetyMode.NONE) { component.get(type, name, parameters) }

    /**
     * @see Component.injectOrNull
     */
    fun <T> InjektTrait.injectOrNull(
        type: Type<T>,
        name: Any? = null,
        parameters: ParametersDefinition? = null
    ): kotlin.Lazy<T?> = lazy(LazyThreadSafetyMode.NONE) { component.getOrNull(type, name, parameters) }
}

/**
 * @see Component.get
 */
inline fun <reified T> InjektTrait.get(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): T = get(type = typeOf(), name = name, parameters = parameters)

/**
 * @see Component.getOrNull
 */
inline fun <reified T> InjektTrait.getOrNull(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): T? = getOrNull(type = typeOf(), name = name, parameters = parameters)

/**
 * @see Component.inject
 */
inline fun <reified T> InjektTrait.inject(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): kotlin.Lazy<T> = inject(type = typeOf(), name = name, parameters = parameters)

/**
 * @see Component.injectOrNull
 */
inline fun <reified T> InjektTrait.injectOrNull(
    name: Any? = null,
    noinline parameters: ParametersDefinition? = null
): kotlin.Lazy<T?> = injectOrNull(type = typeOf(), name = name, parameters = parameters)
