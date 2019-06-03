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

/**
 * Map binding
 */
data class MapBinding<K, V>(
    val key: K,
    val keyType: Type<K>,
    val valueType: Type<V>,
    val mapName: Qualifier?,
    val override: Boolean
) {
    val mapKey = Key(customTypeOf<Map<K, V>>(Map::class, keyType, valueType), mapName)
}

inline fun <reified K, reified V> mapBinding(
    key: K,
    keyType: Type<K> = typeOf(),
    valueType: Type<V> = typeOf(),
    mapName: Qualifier? = null,
    override: Boolean = false
): MapBinding<K, V> = MapBinding(key, keyType, valueType, mapName, override)