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

package com.ivianuu.injekt

import kotlin.reflect.KClass

interface JustInTimeLookupFactory {
    fun <T> findBindingForKey(key: Key): Binding<T>?
}

object CodegenJustInTimeLookupFactory : JustInTimeLookupFactory {

    private val lookups = mutableMapOf<Type<*>, Binding<*>>()

    override fun <T> findBindingForKey(key: Key): Binding<T>? {
        if (key.name != null) return null
        val type = key.type

        var lookup = synchronized(lookups) { lookups[type] }

        if (lookup == null) {
            lookup = findLookup(type.classifier)
            if (lookup != null) {
                synchronized(lookups) {
                    lookups[type] = lookup
                }
            }
        }

        return lookup as? Binding<T>
    }

    private fun findLookup(classifier: KClass<*>) = try {
        val bindingFactoryClass = classifier.java.declaredClasses
            .first { BindingFactory::class.java.isAssignableFrom(it) }
        bindingFactoryClass.declaredFields
            .first { it.type == bindingFactoryClass }
            .also { it.isAccessible = true }
            .get(null)
            .let { it as BindingFactory<*> }
            .create()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
