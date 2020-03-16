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

/**
 * Used by the codegen
 */
interface BindingFactory<T> {
    fun create(): Binding<T>
}

object CodegenJustInTimeBindingFactory : JustInTimeBindingFactory {

    private val bindingFactories = mutableMapOf<Key<*>, Any?>()

    override fun <T> create(key: Key<T>): Binding<T>? {
        if (key.qualifier != Qualifier.None) return null

        var bindingFactoryOrThis: Any? = synchronized(bindingFactories) { bindingFactories[key] }

        if (bindingFactoryOrThis === this) return null

        if (bindingFactoryOrThis == null) {
            bindingFactoryOrThis = findBindingFactory(key.classifier) ?: this
            synchronized(bindingFactories) {
                bindingFactories[key] = bindingFactoryOrThis
            }
        }

        return (bindingFactoryOrThis as? BindingFactory<T>)?.create()
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
