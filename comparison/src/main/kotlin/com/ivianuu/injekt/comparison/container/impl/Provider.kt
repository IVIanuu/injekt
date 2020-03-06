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

package com.ivianuu.injekt.comparison.container.impl

import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.emptyParameters
import com.ivianuu.injekt.keyOf
import com.ivianuu.injekt.typeOf
import org.jetbrains.annotations.Nullable

inline fun <reified T> providerOf(): Container.(Parameters) -> T {
    return {
        val constructor = T::class.java.constructors.first()
        constructor.newInstance(
            *(0 until constructor.parameterCount)
                .asSequence()
                .map { index ->
                    val isNullable = constructor.parameterAnnotations[index]
                        .any { it.annotationClass == Nullable::class }
                    val key = keyOf(typeOf<Any?>(constructor.parameterTypes[index], isNullable))
                    get<Any?>(key)
                }
                .toList()
                .toTypedArray()
        ) as T
    }
}

fun <T> (Container.(Parameters) -> T).invoke(container: Container) = invoke(container, emptyParameters())
