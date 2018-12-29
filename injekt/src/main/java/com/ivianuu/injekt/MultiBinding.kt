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
 * Describes a set binding for a [Declaration]
 */
data class SetBinding(
    val type: KClass<*>,
    val name: String? = null
)

/**
 * Returns a new [SetBinding] for [type] and [name]
 */
fun setBinding(type: KClass<*>, name: String? = null) =
    SetBinding(type, name)

/**
 * Returns a new [SetBinding] for [T] and [name]
 */
inline fun <reified T : Any> setBinding(name: String? = null) =
    setBinding(T::class, name)

/**
 * Describes a map binding for a [Declaration]
 */
data class MapBinding(
    val type: KClass<*>,
    val key: Any,
    val name: String? = null
)

/**
 * Returns a new [MapBinding] for [type], [key] and [name]
 */
fun mapBinding(type: KClass<*>, key: Any, name: String? = null) =
    MapBinding(type, key, name)

/**
 * Returns a new [MapBinding] for [T], [key] and [name]
 */
inline fun <reified T : Any> mapBinding(key: Any, name: String? = null) =
    mapBinding(T::class, key, name)