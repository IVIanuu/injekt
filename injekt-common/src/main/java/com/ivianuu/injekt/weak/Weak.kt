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

package com.ivianuu.injekt.weak

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.InjektPlugins
import com.ivianuu.injekt.Instance
import com.ivianuu.injekt.Kind
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.bind
import com.ivianuu.injekt.logger
import com.ivianuu.injekt.scopeName
import com.ivianuu.injekt.typeOf
import java.lang.ref.WeakReference

/**
 * This caches the value via a [WeakReference] and creates it again
 * if the reference was garbage collected
 */
object WeakKind : Kind {
    override fun <T> createInstance(binding: Binding<T>): Instance<T> = WeakInstance(binding)
    override fun toString() = "Weak"
}

inline fun <reified T> Module.weak(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): Binding<T> = weak(typeOf(), name, override, definition)

fun <T> Module.weak(
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> = bind(WeakKind, type, name, override, definition)

@Target(AnnotationTarget.CLASS)
annotation class Weak

private class WeakInstance<T>(override val binding: Binding<T>) : Instance<T>() {

    private var _value: WeakReference<T>? = null

    override fun get(parameters: ParametersDefinition?): T {
        val value = _value?.get()

        return if (value != null) {
            InjektPlugins.logger?.info("${context.component.scopeName()} Return existing weak instance $binding")
            value
        } else {
            InjektPlugins.logger?.info("${context.component.scopeName()} Create weak instance $binding")
            create(parameters).also { _value = WeakReference(it) }
        }
    }

}