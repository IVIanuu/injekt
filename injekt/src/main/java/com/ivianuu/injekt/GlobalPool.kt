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

internal object GlobalPool {

    private val bindings = mutableMapOf<Key, Binding<*>>()

    fun <T> get(key: Key): Binding<T>? {
        var binding = bindings[key]

        if (binding == null) {
            binding = findCreator(key.type.raw)

            if (binding != null) {
                bindings[key] = binding
            }
        }

        return binding as? Binding<T>
    }

    private fun findCreator(type: KClass<*>) = try {
        val creatorName = type.java.name
            .replace("\$", "_") + "__Creator"
        val creatorType = Class.forName(creatorName)
        val creator = creatorType.newInstance() as Creator<*>
        creator.create()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}