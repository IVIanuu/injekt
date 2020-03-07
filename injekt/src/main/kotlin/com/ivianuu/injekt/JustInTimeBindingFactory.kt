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

interface JustInTimeBindingFactory {
    fun <T> findBinding(key: Key<T>): Binding<T>?
}

object CodegenJustInTimeBindingFactory : JustInTimeBindingFactory {

    private val bindingFactories = mutableMapOf<Key<*>, BindingFactory<*>>()

    override fun <T> findBinding(key: Key<T>): Binding<T>? {
        if (key.name != null) return null

        var bindingFactory = synchronized(bindingFactories) { bindingFactories[key] }

        if (bindingFactory == null) {
            bindingFactory = findBindingFactory(key.classifier)
            if (bindingFactory != null) {
                synchronized(bindingFactories) {
                    bindingFactories[key] = bindingFactory
                }
            }
        }

        return bindingFactory?.create() as? Binding<T>
    }

    private fun findBindingFactory(classifier: KClass<*>) = try {
        val bindingFactoryClass = classifier.java.declaredClasses
            .first { BindingFactory::class.java.isAssignableFrom(it) }
        bindingFactoryClass.declaredFields
            .first { it.type == bindingFactoryClass }
            .also { it.isAccessible = true }
            .get(null) as BindingFactory<*>
    } catch (e: Exception) {
        null
    }
}
