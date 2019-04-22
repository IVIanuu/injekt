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

/**
 * Attribute key for [MapBinding]s
 */
const val KEY_MAP_BINDINGS = "map_bindings"

/**
 * Map name
 */
class MapName<K, V>

/**
 * Map binding
 */
data class MapBinding<K, V>(
    val mapName: MapName<K, V>,
    val key: K,
    val override: Boolean
)