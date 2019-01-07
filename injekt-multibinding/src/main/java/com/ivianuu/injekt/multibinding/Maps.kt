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
import com.ivianuu.injekt.ModuleContext
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.getOrSet
import java.util.*
import kotlin.reflect.KClass

/**
 * Attribute key for [BeanDefinition] which contains any [Map] binding of the definition
 */
const val KEY_MAP_BINDINGS = "mapBindings"

/**
 * Declares a empty set binding with the scopeId [mapName]
 * This is useful for retrieving a [MultiBindingMap] even if no [BeanDefinition] was bound into it
 */
fun ModuleContext.mapBinding(mapName: String) {
    factory(name = mapName, override = true) { MultiBindingMap<Any, Any>(emptyMap()) }
}

/**
 * Binds this [BeanDefinition] into a [Map] named [Pair.first] with the key [Pair.second]
 */
infix fun <T : Any> BindingContext<T>.bindIntoMap(pair: Pair<String, Any>): BindingContext<T> {
    val (mapName, mapKey) = pair

    definition.attributes.getOrSet(KEY_MAP_BINDINGS) { mutableMapOf<String, Any>() }[mapName] =
            mapKey

    moduleContext.factory(name = mapName, override = true) {
        component.beanRegistry
            .getAllDefinitions()
            .mapNotNull { definition ->
                definition.attributes.get<Map<String, Any>>(KEY_MAP_BINDINGS)
                    ?.get(mapName)?.let { it to definition }
            }
            .toMap()
            .mapValues { it.value as BeanDefinition<Any> }
            .let { MultiBindingMap(it) }
    }

    return this
}

/**
 * Binds a already existing [BeanDefinition] into a [Map] named [Pair.first] with the key [Pair.second]
 */
inline fun <reified T : Any> ModuleContext.bindIntoMap(
    mapName: String,
    key: Any,
    implementationName: String? = null
): BindingContext<T> = bindIntoMap(T::class, mapName, key, implementationName)

/**
 * Binds a already existing [BeanDefinition] into a [Map] named [Pair.first] with the key [Pair.second]
 */
fun <T : Any> ModuleContext.bindIntoMap(
    implementationType: KClass<T>,
    mapName: String,
    key: Any,
    implementationName: String? = null
): BindingContext<T> =
    factory(implementationType, UUID.randomUUID().toString()) {
        get(implementationType, implementationName)
    } bindIntoMap (mapName to key)

/**
 * Returns a multi bound [Map] for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K : Any, T : Any> Component.getMap(
    name: String,
    parameters: ParametersDefinition? = null
): Map<K, T> =
    get<MultiBindingMap<K, T>>(name).toMap(parameters)

/**
 * Returns multi bound [Map] of [Lazy]s for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K : Any, T : Any> Component.getLazyMap(
    name: String,
    parameters: ParametersDefinition? = null
): Map<K, Lazy<T>> =
    get<MultiBindingMap<K, T>>(name).toLazyMap(parameters)

/**
 * Returns a multi bound [Map] of [Provider]s for [K], [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <K : Any, T : Any> Component.getProviderMap(
    name: String,
    defaultParameters: ParametersDefinition? = null
): Map<K, Provider<T>> =
    get<MultiBindingMap<K, T>>(name).toProviderMap(defaultParameters)

/**
 * Lazily Returns a multi bound [Map] for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K : Any, T : Any> Component.injectMap(
    name: String,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, T>> =
    lazy { getMap<K, T>(name, parameters) }

/**
 * LazilyReturns multi bound [Map] of [Lazy]s for [K], [T] [name] and passes [parameters] to any of the entries
 */
fun <K : Any, T : Any> Component.injectLazyMap(
    name: String,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, Lazy<T>>> =
    lazy { getLazyMap<K, T>(name, parameters) }

/**
 * Lazily Returns a multi bound [Map] of [Provider]s for [K], [T] [name] and passes [defaultParameters] to each [Provider]
 */
fun <K : Any, T : Any> Component.injectProviderMap(
    name: String,
    defaultParameters: ParametersDefinition? = null
): Lazy<Map<K, Provider<T>>> =
    lazy { getProviderMap<K, T>(name, defaultParameters) }

/** Calls trough [Component.getMap] */
fun <K : Any, T : Any> InjektTrait.getMap(
    name: String,
    parameters: ParametersDefinition? = null
): Map<K, T> =
    component.getMap<K, T>(name, parameters)

/** Calls trough [Component.getLazyMap] */
fun <K : Any, T : Any> InjektTrait.getLazyMap(
    name: String,
    parameters: ParametersDefinition? = null
): Map<K, Lazy<T>> =
    component.getLazyMap<K, T>(name, parameters)

/** Calls trough [Component.getProviderMap] */
fun <K : Any, T : Any> InjektTrait.getProviderMap(
    name: String,
    defaultParameters: ParametersDefinition? = null
): Map<K, Provider<T>> =
    component.getProviderMap<K, T>(name, defaultParameters)

/** Calls trough [Component.injectMap] */
fun <K : Any, T : Any> InjektTrait.injectMap(
    name: String,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, T>> =
    lazy { component.getMap<K, T>(name, parameters) }

/** Calls trough [Component.injectLazyMap] */
fun <K : Any, T : Any> InjektTrait.injectLazyMap(
    name: String,
    parameters: ParametersDefinition? = null
): Lazy<Map<K, Lazy<T>>> =
    lazy { component.getLazyMap<K, T>(name, parameters) }

/** Calls trough [Component.injectProviderMap] */
fun <K : Any, T : Any> InjektTrait.injectProviderMap(
    name: String,
    defaultParameters: ParametersDefinition? = null
): Lazy<Map<K, Provider<T>>> =
    lazy { component.getProviderMap<K, T>(name, defaultParameters) }