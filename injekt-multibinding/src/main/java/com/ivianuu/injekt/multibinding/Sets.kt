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
fun ModuleContext.setBinding(setName: String) {
    factory(name = setName, override = true) {
        MultiBindingSet<Any>(emptySet())
    }
}

/**
 * Binds this [BeanDefinition] into a [Set] named [setName]
 */
infix fun <T : Any> BindingContext<T>.bindIntoSet(setName: String) = apply {
    definition.attributes.getOrSet(KEY_SET_BINDINGS) { mutableSetOf<String>() }.add(setName)

    moduleContext.factory(name = setName, override = true) {
        component.beanRegistry
            .getAllDefinitions()
            .filter { it.attributes.get<Set<String>>(KEY_SET_BINDINGS)?.contains(setName) == true }
            .map { it as BeanDefinition<T> }
            .toSet()
            .let { MultiBindingSet(it) }
    }
}

/**
 * Binds a already existing [BeanDefinition] into a [Set] named [setName]
 */
inline fun <reified T : Any> ModuleContext.bindIntoSet(
    setName: String,
    implementationName: String? = null
) = bindIntoSet(T::class, setName, implementationName)

/**
 * Binds a already existing [BeanDefinition] into a [Set] named [setName]
 */
fun <T : Any> ModuleContext.bindIntoSet(
    implementationType: KClass<T>,
    setName: String,
    implementationName: String? = null
) =
    factory(implementationType, UUID.randomUUID().toString()) {
        get(implementationType, implementationName)
    } bindIntoSet setName

/**
 * Returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T : Any> Component.getSet(name: String, parameters: ParametersDefinition? = null) =
    get<MultiBindingSet<T>>(name).toSet(parameters)

/**
 * Returns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T : Any> Component.getLazySet(name: String, parameters: ParametersDefinition? = null) =
    get<MultiBindingSet<T>>(name).toLazySet(parameters)

/**
 * Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T : Any> Component.getProviderSet(
    name: String,
    defaultParameters: ParametersDefinition? = null
) =
    get<MultiBindingSet<T>>(name).toProviderSet(defaultParameters)

/**
 * Lazily Returns a multi bound [Set] for [T] [name] and passes [parameters] to any of the entries
 */
fun <T : Any> Component.injectSet(name: String, parameters: ParametersDefinition? = null) =
    lazy { getSet<T>(name, parameters) }

/**
 * LazilyReturns multi bound [Set] of [Lazy]s for [T] [name] and passes [parameters] to any of the entries
 */
fun <T : Any> Component.injectLazySet(name: String, parameters: ParametersDefinition? = null) =
    lazy { getLazySet<T>(name, parameters) }

/**
 * Lazily Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <T : Any> Component.injectProviderSet(
    name: String,
    defaultParameters: ParametersDefinition? = null
) =
    lazy { getProviderSet<T>(name, defaultParameters) }

/** Calls trough [Component.getSet] */
fun <T : Any> InjektTrait.getSet(name: String, parameters: ParametersDefinition? = null) =
    component.getSet<T>(name, parameters)

/** Calls trough [Component.getLazySet] */
fun <T : Any> InjektTrait.getLazySet(name: String, parameters: ParametersDefinition? = null) =
    component.getLazySet<T>(name, parameters)

/** Calls trough [Component.getProviderSet] */
fun <T : Any> InjektTrait.getProviderSet(
    name: String,
    defaultParameters: ParametersDefinition? = null
) =
    component.getProviderSet<T>(name, defaultParameters)

/** Calls trough [Component.injectSet] */
fun <T : Any> InjektTrait.injectSet(name: String, parameters: ParametersDefinition? = null) =
    lazy { component.getSet<T>(name, parameters) }

/** Calls trough [Component.injectLazySet] */
fun <T : Any> InjektTrait.injectLazySet(name: String, parameters: ParametersDefinition? = null) =
    lazy { component.getLazySet<T>(name, parameters) }

/** Calls trough [Component.injectProviderSet] */
fun <T : Any> InjektTrait.injectProviderSet(
    name: String,
    defaultParameters: ParametersDefinition? = null
) =
    lazy { component.getLazySet<T>(name, defaultParameters) }