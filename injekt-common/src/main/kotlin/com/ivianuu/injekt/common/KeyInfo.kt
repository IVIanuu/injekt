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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.ForKey
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.keyOf
import kotlin.reflect.KClass

data class KeyInfo<T>(val key: Key<T>) {
    val classifierName: String by lazy(LazyThreadSafetyMode.NONE) {
        val withoutAnnotations = if (key.value.startsWith("["))
            key.value.removeRange(0 until key.value.indexOf(']')) else key.value
        if (withoutAnnotations.contains("<"))
            withoutAnnotations.split("<")[0] else withoutAnnotations
            .removeSuffix("?")
    }
    val classifier: KClass<*> by lazy(LazyThreadSafetyMode.NONE) {
        Class.forName(classifierName).kotlin
    }

    val arguments: List<KeyInfo<*>> by lazy(LazyThreadSafetyMode.NONE) {
        if (!key.value.contains("<")) return@lazy emptyList()
        key.value
            .substring(key.value.indexOf("<"), key.value.lastIndexOf(">"))
            .split(", ")
            .filter { it.isNotEmpty() }
            .map { KeyInfo<Any?>(Key(it)) }
    }

    val isMarkedNullable: Boolean get() = key.value.endsWith("?")
}

inline fun <T> Key<T>.toKeyInfo() = KeyInfo(this)

inline fun <@ForKey T> keyInfoOf(): KeyInfo<T> = keyOf<T>().toKeyInfo()
