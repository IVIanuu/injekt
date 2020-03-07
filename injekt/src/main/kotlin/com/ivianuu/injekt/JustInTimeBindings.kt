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
    fun <T> findBinding(type: Type<T>): Binding<T>?
}

object CodegenJustInTimeLookupFactory : JustInTimeLookupFactory {

    private val bindingFactories = mutableMapOf<Type<*>, BindingFactory<*>>()

    override fun <T> findBinding(type: Type<T>): Binding<T>? {
        var bindingFactory = synchronized(bindingFactories) { bindingFactories[type] }

        if (bindingFactory == null) {
            bindingFactory = findBindingFactory(type.classifier)
            if (bindingFactory != null) {
                synchronized(bindingFactories) {
                    bindingFactories[type] = bindingFactory
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
