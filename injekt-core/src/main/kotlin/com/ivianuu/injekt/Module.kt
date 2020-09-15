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

import java.util.ServiceLoader
import kotlin.reflect.KClass

annotation class Module(val contextName: KClass<*> = AnyContext::class) {
    interface Registrar {
        fun register()
    }
}

object ModuleRegistry {
    private val _modules = mutableMapOf<Key<ContextName>, MutableList<ContextBuilder.() -> Unit>>()
    internal val modules: MutableMap<Key<ContextName>, MutableList<ContextBuilder.() -> Unit>>
        get() {
            var initialized = _initialized
            if (!initialized) {
                synchronized(this) {
                    initialized = _initialized
                    if (!initialized) {
                        _initialized = true
                        ServiceLoader.load(Module.Registrar::class.java)
                            .forEach { it.register() }
                    }
                }
            }
            return _modules
        }

    private var _initialized = false

    fun module(
        contextName: Key<ContextName>,
        block: ContextBuilder.() -> Unit
    ) {
        modules.getOrPut(contextName) { mutableListOf() } += block
    }
}
