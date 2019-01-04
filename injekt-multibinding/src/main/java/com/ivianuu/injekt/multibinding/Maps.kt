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
 * Declares a empty set binding with the name [mapName]
 * This is useful for retrieving a [MultiBindingMap] even if no [BeanDefinition] was bound into it
 */
fun <K : Any, T : Any> ModuleContext.mapBinding(mapName: String) {
    factory(name = mapName, override = true) {
        MultiBindingMap<K, T>(emptyMap())
    }
}

/**
 * Binds this [BeanDefinition] into a [Map] named [Pair.first] with the key [Pair.second]
 */
infix fun <K : Any, T : Any, S : T> BeanDefinition<S>.intoMap(pair: Pair<String, K>) =
    apply {
        val (mapName, mapKey) = pair

        attributes.getOrSet(KEY_MAP_BINDINGS) { mutableMapOf<String, Any>() }[mapName] = mapKey

        moduleContext.factory(name = mapName, override = true) {
            component.beanRegistry
                .getAllDefinitions()
                .mapNotNull { definition ->
                    definition.attributes.get<Map<String, Any>>(KEY_MAP_BINDINGS)
                        ?.get(mapName)?.let { it to definition }
                }
                .toMap()
                .mapKeys { it.key as K }
                .mapValues { it.value as BeanDefinition<T> }
                .let { MultiBindingMap(it) }
        }
    }

/**
 * Binds a already existing [BeanDefinition] into a [Map] named [Pair.first] with the key [Pair.second]
 */
inline fun <K : Any, reified T : Any, reified S : T> ModuleContext.bindIntoMap(
    mapName: String,
    key: K,
    declarationName: String? = null
) = bindIntoMap(T::class, S::class, mapName, key, declarationName)

/**
 * Binds a already existing [BeanDefinition] into a [Map] named [Pair.first] with the key [Pair.second]
 */
fun <K : Any, T : Any, S : T> ModuleContext.bindIntoMap(
    mapType: KClass<T>,
    implementationType: KClass<S>,
    mapName: String,
    key: K,
    implementationName: String? = null
) =
    factory(mapType, UUID.randomUUID().toString()) {
        get(implementationType, implementationName)
    } intoMap (mapName to key)