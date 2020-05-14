/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.internal

import com.ivianuu.injekt.Provider

class MapProvider<K, V>(private val providers: Map<K, () -> V>) : @Provider () -> Map<K, V> {
    override fun invoke(): Map<K, V> =
        providers.mapValues { it.value() }

    companion object {
        private val EMPTY = MapProvider<Any?, Any?>(emptyMap())

        fun <K, V> create(
            pair: Pair<K, () -> V>
        ): MapProvider<K, V> = MapProvider(mapOf(pair))

        fun <K, V> create(
            vararg pairs: Pair<K, () -> V>
        ): MapProvider<K, V> = MapProvider(mapOf(*pairs))

        fun <K, V> empty(): MapProvider<K, V> = EMPTY as MapProvider<K, V>

    }
}
