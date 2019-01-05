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
 * Declares a empty set binding with the name [setName]
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
infix fun <T : Any> BeanDefinition<T>.bindIntoSet(setName: String) = apply {
    attributes.getOrSet(KEY_SET_BINDINGS) { mutableSetOf<String>() }.add(setName)

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
inline fun <reified T : Any, reified S : T> ModuleContext.bindIntoSet(
    setName: String,
    declarationName: String? = null
) = bindIntoSet(T::class, S::class, setName, declarationName)

/**
 * Binds a already existing [BeanDefinition] into a [Set] named [setName]
 */
fun <T : Any, S : T> ModuleContext.bindIntoSet(
    setType: KClass<T>,
    implementationType: KClass<S>,
    setName: String,
    implementationName: String? = null
) =
    factory(setType, UUID.randomUUID().toString()) {
        get(implementationType, implementationName)
    } bindIntoSet setName

/**
 * Returns a multi bound [Set] for [T] [name] and passes [params] to any of the entries
 */
fun <T : Any> Component.getSet(name: String, params: ParamsDefinition? = null) =
    get<MultiBindingSet<T>>(name).toSet(params)

/**
 * Returns multi bound [Set] of [Lazy]s for [T] [name] and passes [params] to any of the entries
 */
fun <T : Any> Component.getLazySet(name: String, params: ParamsDefinition? = null) =
    get<MultiBindingSet<T>>(name).toLazySet(params)

/**
 * Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParams] to each [Provider]
 */
fun <T : Any> Component.getProviderSet(name: String, defaultParams: ParamsDefinition? = null) =
    get<MultiBindingSet<T>>(name).toProviderSet(defaultParams)

/**
 * Lazily Returns a multi bound [Set] for [T] [name] and passes [params] to any of the entries
 */
fun <T : Any> Component.injectSet(name: String, params: ParamsDefinition? = null) =
    lazy { getSet<T>(name, params) }

/**
 * LazilyReturns multi bound [Set] of [Lazy]s for [T] [name] and passes [params] to any of the entries
 */
fun <T : Any> Component.injectLazySet(name: String, params: ParamsDefinition? = null) =
    lazy { getLazySet<T>(name, params) }

/**
 * Lazily Returns a multi bound [Set] of [Provider]s for [T] [name] and passes [defaultParams] to each [Provider]
 */
fun <T : Any> Component.injectProviderSet(name: String, defaultParams: ParamsDefinition? = null) =
    lazy { getProviderSet<T>(name, defaultParams) }

/** Calls trough [Component.getSet] */
fun <T : Any> InjektTrait.getSet(name: String, params: ParamsDefinition? = null) =
    component.getSet<T>(name, params)

/** Calls trough [Component.getLazySet] */
fun <T : Any> InjektTrait.getLazySet(name: String, params: ParamsDefinition? = null) =
    component.getLazySet<T>(name, params)

/** Calls trough [Component.getProviderSet] */
fun <T : Any> InjektTrait.getProviderSet(name: String, defaultParams: ParamsDefinition? = null) =
    component.getProviderSet<T>(name, defaultParams)

/** Calls trough [Component.injectSet] */
fun <T : Any> InjektTrait.injectSet(name: String, params: ParamsDefinition? = null) =
    lazy { component.getSet<T>(name, params) }

/** Calls trough [Component.injectLazySet] */
fun <T : Any> InjektTrait.injectLazySet(name: String, params: ParamsDefinition? = null) =
    lazy { component.getLazySet<T>(name, params) }

/** Calls trough [Component.injectProviderSet] */
fun <T : Any> InjektTrait.injectProviderSet(name: String, defaultParams: ParamsDefinition? = null) =
    lazy { component.getLazySet<T>(name, defaultParams) }