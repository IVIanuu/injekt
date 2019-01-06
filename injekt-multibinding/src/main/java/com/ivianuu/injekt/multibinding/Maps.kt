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
infix fun <T : Any> BindingContext<T>.bindIntoMap(pair: Pair<String, Any>) =
    apply {
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
    }

/**
 * Binds a already existing [BeanDefinition] into a [Map] named [Pair.first] with the key [Pair.second]
 */
inline fun <reified T : Any> ModuleContext.bindIntoMap(
    mapName: String,
    key: Any,
    implementationName: String? = null
) = bindIntoMap(T::class, mapName, key, implementationName)

/**
 * Binds a already existing [BeanDefinition] into a [Map] named [Pair.first] with the key [Pair.second]
 */
fun <T : Any> ModuleContext.bindIntoMap(
    implementationType: KClass<T>,
    mapName: String,
    key: Any,
    implementationName: String? = null
) =
    factory(implementationType, UUID.randomUUID().toString()) {
        get(implementationType, implementationName)
    } bindIntoMap (mapName to key)

/**
 * Returns a multi bound [Map] for [K], [T] [name] and passes [params] to any of the entries
 */
fun <K : Any, T : Any> Component.getMap(name: String, params: ParamsDefinition? = null) =
    get<MultiBindingMap<K, T>>(name).toMap(params)

/**
 * Returns multi bound [Map] of [Lazy]s for [K], [T] [name] and passes [params] to any of the entries
 */
fun <K : Any, T : Any> Component.getLazyMap(name: String, params: ParamsDefinition? = null) =
    get<MultiBindingMap<K, T>>(name).toLazyMap(params)

/**
 * Returns a multi bound [Map] of [Provider]s for [K], [T] [name] and passes [defaultParams] to each [Provider]
 */
fun <K : Any, T : Any> Component.getProviderMap(
    name: String,
    defaultParams: ParamsDefinition? = null
) =
    get<MultiBindingMap<K, T>>(name).toProviderMap(defaultParams)

/**
 * Lazily Returns a multi bound [Map] for [K], [T] [name] and passes [params] to any of the entries
 */
fun <K : Any, T : Any> Component.injectMap(name: String, params: ParamsDefinition? = null) =
    lazy { getMap<K, T>(name, params) }

/**
 * LazilyReturns multi bound [Map] of [Lazy]s for [K], [T] [name] and passes [params] to any of the entries
 */
fun <K : Any, T : Any> Component.injectLazyMap(name: String, params: ParamsDefinition? = null) =
    lazy { getLazyMap<K, T>(name, params) }

/**
 * Lazily Returns a multi bound [Map] of [Provider]s for [K], [T] [name] and passes [defaultParams] to each [Provider]
 */
fun <K : Any, T : Any> Component.injectProviderMap(
    name: String,
    defaultParams: ParamsDefinition? = null
) =
    lazy { getProviderMap<K, T>(name, defaultParams) }

/** Calls trough [Component.getMap] */
fun <K : Any, T : Any> InjektTrait.getMap(name: String, params: ParamsDefinition? = null) =
    component.getMap<K, T>(name, params)

/** Calls trough [Component.getLazyMap] */
fun <K : Any, T : Any> InjektTrait.getLazyMap(name: String, params: ParamsDefinition? = null) =
    component.getLazyMap<K, T>(name, params)

/** Calls trough [Component.getProviderMap] */
fun <K : Any, T : Any> InjektTrait.getProviderMap(
    name: String,
    defaultParams: ParamsDefinition? = null
) =
    component.getProviderMap<K, T>(name, defaultParams)

/** Calls trough [Component.injectMap] */
fun <K : Any, T : Any> InjektTrait.injectMap(name: String, params: ParamsDefinition? = null) =
    lazy { component.getMap<K, T>(name, params) }

/** Calls trough [Component.injectLazyMap] */
fun <K : Any, T : Any> InjektTrait.injectLazyMap(name: String, params: ParamsDefinition? = null) =
    lazy { component.getLazyMap<K, T>(name, params) }

/** Calls trough [Component.injectProviderMap] */
fun <K : Any, T : Any> InjektTrait.injectProviderMap(
    name: String,
    defaultParams: ParamsDefinition? = null
) =
    lazy { component.getProviderMap<K, T>(name, defaultParams) }