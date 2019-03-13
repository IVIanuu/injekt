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

package com.ivianuu.injekt.util

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Instance
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import kotlin.reflect.KClass

inline fun <reified T> Component.getBinding(
    qualifier: Qualifier? = null
): Binding<T> = getBinding(T::class, qualifier)

fun <T> Component.getBinding(
    type: KClass<*>,
    qualifier: Qualifier? = null
): Binding<T> {
    val key = Key(type, qualifier)
    return getBindings().firstOrNull { it.key == key } as? Binding<T>
        ?: error("binding not found")
}

fun <T> Component.getInstance(type: KClass<*>, qualifier: Qualifier? = null): Instance<T> {
    val key = Key(type, qualifier)
    return getInstances().firstOrNull { it.binding.key == key } as? Instance<T>
        ?: error("instance not found")
}