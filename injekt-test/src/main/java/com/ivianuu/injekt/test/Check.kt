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

package com.ivianuu.injekt.test

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingNotFoundException
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.InjektPlugins
import com.ivianuu.injekt.InstanceCreationException
import com.ivianuu.injekt.OverrideException
import com.ivianuu.injekt.logger
import org.mockito.Mockito

/**
 * Checks if all [Binding]s can be resolved
 */
fun Component.check() {
    setSandboxBindings()
    getBindings().forEach {
        get<Any?>(
            it.type,
            it.name
        ).also { instance -> println("got $instance") }
    }
}

fun Component.setSandboxBindings() {
    getDependencies().forEach(Component::setSandboxBindings)
    getBindings().forEach {
        println("clone and save for sandbox $it")
        addBinding(it.cloneForSandbox())
    }
}

fun <T> Binding<T>.cloneForSandbox(): Binding<T> {
    val sandboxDefinition: Definition<T> = { parameters ->
        try {
            definition.invoke(this, parameters)
        } catch (e: Exception) {
            when (e) {
                is BindingNotFoundException, is InstanceCreationException, is OverrideException -> {
                    throw BrokenBindingException("Definition $this is broken due to error : $e")
                }
                else -> InjektPlugins.logger?.debug("sandbox resolution continue on caught error: $e")
            }
        }
        Mockito.mock(type.java) as T
    }

    return copy(override = true, definition = sandboxDefinition)
}

class BrokenBindingException(msg: String) : Exception(msg)