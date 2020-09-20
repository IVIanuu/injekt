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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import org.jetbrains.kotlin.com.intellij.util.keyFMap.KeyFMap
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import java.util.WeakHashMap

@Given(ApplicationContext::class)
class WeakBindingTrace {
    private val map = WeakHashMap<Any, KeyFMap>()

    fun <K : IrAttributeContainer, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
        var holder = map[key.attributeOwnerId] ?: KeyFMap.EMPTY_MAP
        val prev = holder.get(slice.key)
        if (prev != null) {
            holder = holder.minus(slice.key)
        }
        holder = holder.plus(slice.key, value)
        map[key.attributeOwnerId] = holder
    }

    operator fun <K : IrAttributeContainer, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        return map[key.attributeOwnerId]?.get(slice.key)
    }
}
