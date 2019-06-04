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
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.typeOf

inline fun <reified T> Component.getBinding(
    name: Qualifier? = null
): Binding<T> = getBinding(typeOf(), name)

fun <T> Component.getBinding(
    type: Type<T>,
    name: Qualifier? = null
): Binding<T> {
    val key = Key(type, name)
    return instances.entries.firstOrNull { it.key == key }?.value?.binding as? Binding<T>
        ?: error("binding not found")
}