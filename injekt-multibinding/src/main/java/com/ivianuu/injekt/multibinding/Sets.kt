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
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.getOrSet
import java.util.*
import kotlin.reflect.KClass

/**
 * Attribute key for [BeanDefinition] which contains any [Set] binding of the definition
 */
const val KEY_SET_BINDINGS = "setBindings"

/**
 * Declares a empty set binding with the scopeId [setName]
 * This is useful for retrieving a [MultiBindingSet] even if no [BeanDefinition] was bound into it
 */
fun Module.setBinding(setName: String) {
    factory(name = setName, override = true) {
        MultiBindingSet<Any>(component, emptySet())
    }
}

/**
 * Binds this [BeanDefinition] into a [Set] named [setName]
 */
infix fun <T : Any> BindingContext<T>.bindIntoSet(setName: String): BindingContext<T> {
    definition.attributes.getOrSet(KEY_SET_BINDINGS) { mutableSetOf<String>() }.add(setName)

    module.factory(name = setName, override = true) {
        val allDefinitions = component.getDefinitions() + (
                component.getDependencies().flatMap { it.getDefinitions() })

        allDefinitions
            .filter { it.attributes.get<Set<String>>(KEY_SET_BINDINGS)?.contains(setName) == true }
            .map { it as BeanDefinition<T> }
            .toSet()
            .let { MultiBindingSet(component, it) }
    }

    return this
}

/**
 * Binds a already existing [BeanDefinition] into a [Set] named [setName]
 */
inline fun <reified T : Any> Module.bindIntoSet(
    setName: String,
    implementationName: String? = null
): BindingContext<T> = bindIntoSet(T::class, setName, implementationName)

/**
 * Binds a already existing [BeanDefinition] into a [Set] named [setName]
 */
fun <T : Any> Module.bindIntoSet(
    implementationType: KClass<T>,
    setName: String,
    implementationName: String? = null
): BindingContext<T> {
    // we use a unique id here to make sure that the binding does not collide with any user config
    return factory(implementationType, UUID.randomUUID().toString()) {
        get(implementationType, implementationName)
    } bindIntoSet setName
}

/**
 * Returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T : Any> Component.getSet(name: String, parameters: ParametersDefinition? = null): Set<T> =
    get<MultiBindingSet<T>>(name).toSet(parameters)

/**
 * Returns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T : Any> Component.getLazySet(
    name: String,
    parameters: ParametersDefinition? = null
): Set<Lazy<T>> =
    get<MultiBindingSet<T>>(name).toLazySet(parameters)

/**
 * Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T : Any> Component.getProviderSet(
    name: String,
    defaultParameters: ParametersDefinition? = null
): Set<Provider<T>> = get<MultiBindingSet<T>>(name).toProviderSet(defaultParameters)

/**
 * Lazily Returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T : Any> Component.injectSet(
    name: String,
    parameters: ParametersDefinition? = null
): Lazy<Set<T>> = lazy { getSet<T>(name, parameters) }

/**
 * LazilyReturns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T : Any> Component.injectLazySet(
    name: String,
    parameters: ParametersDefinition? = null
): Lazy<Set<Lazy<T>>> =
    lazy { getLazySet<T>(name, parameters) }

/**
 * Lazily Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T : Any> Component.injectProviderSet(
    name: String,
    defaultParameters: ParametersDefinition? = null
): Lazy<Set<Provider<T>>> =
    lazy { getProviderSet<T>(name, defaultParameters) }

/** Calls trough [Component.getSet] */
fun <T : Any> InjektTrait.getSet(name: String, parameters: ParametersDefinition? = null): Set<T> =
    component.getSet(name, parameters)

/** Calls trough [Component.getLazySet] */
fun <T : Any> InjektTrait.getLazySet(
    name: String,
    parameters: ParametersDefinition? = null
): Set<Lazy<T>> =
    component.getLazySet(name, parameters)

/** Calls trough [Component.getProviderSet] */
fun <T : Any> InjektTrait.getProviderSet(
    name: String,
    defaultParameters: ParametersDefinition? = null
): Set<Provider<T>> =
    component.getProviderSet(name, defaultParameters)

/** Calls trough [Component.injectSet] */
fun <T : Any> InjektTrait.injectSet(
    name: String,
    parameters: ParametersDefinition? = null
): Lazy<Set<T>> =
    lazy { component.getSet<T>(name, parameters) }

/** Calls trough [Component.injectLazySet] */
fun <T : Any> InjektTrait.injectLazySet(
    name: String,
    parameters: ParametersDefinition? = null
): Lazy<Set<Lazy<T>>> =
    lazy { component.getLazySet<T>(name, parameters) }

/** Calls trough [Component.injectProviderSet] */
fun <T : Any> InjektTrait.injectProviderSet(
    name: String,
    defaultParameters: ParametersDefinition? = null
): Lazy<Set<Provider<T>>> =
    lazy { component.getProviderSet<T>(name, defaultParameters) }