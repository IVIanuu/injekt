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

internal object JustInTimeBindings {

    private val factories = hashMapOf<Key, Binding<*>>()

    fun <T> find(key: Key): Binding<T>? {
        var factory = factories[key]

        if (factory == null) {
            factory = findFactory(key.type.rawJava)
            if (factory != null) factories[key] = factory
        }

        return factory as? Binding<T>
    }

    private fun findFactory(type: Class<*>) = try {
        val bindingClass = Class.forName(type.name + "__Binding")
        bindingClass.declaredFields.first()
            .also { it.isAccessible = true }
            .get(null) as Binding<*>
    } catch (e: Exception) {
        null
    }
}