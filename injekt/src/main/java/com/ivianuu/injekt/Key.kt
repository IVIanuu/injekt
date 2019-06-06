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

data class Key internal constructor(val type: Class<*>, val name: Qualifier? = null) {
    override fun toString(): String {
        return "Key(" +
                "type=$type, " +
                "name=$name)"
    }
}

fun keyOf(type: KClass<*>, name: Qualifier? = null): Key = Key(type.java, name)

inline fun <reified T> keyOf(name: Qualifier? = null): Key = keyOf(T::class, name)