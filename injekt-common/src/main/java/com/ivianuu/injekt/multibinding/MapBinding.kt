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

import com.ivianuu.injekt.Qualifier
import kotlin.reflect.KClass

/**
 * Attribute key for [MapBinding]s
 */
const val KEY_MAP_BINDINGS = "map_bindings"

/**
 * Map name
 */
interface MapName<K, V> : Qualifier

// todo find a better name
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.VALUE_PARAMETER)
annotation class BindingMap(val mapName: KClass<out MapName<*, *>>)

/**
 * Map binding
 */
data class MapBinding<K, V>(val mapName: MapName<K, V>, val key: K)