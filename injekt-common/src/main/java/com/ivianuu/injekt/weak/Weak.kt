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
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.ModuleBuilder
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.UnlinkedBinding
import com.ivianuu.injekt.UnlinkedDefinitionBinding
import com.ivianuu.injekt.add
import com.ivianuu.injekt.typeOf
import java.lang.ref.WeakReference

inline fun <reified T> ModuleBuilder.weak(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = weak(typeOf(), name, override, definition)

fun <T> ModuleBuilder.weak(
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> =
    add(UnlinkedDefinitionBinding(definition).asWeakBinding(), type, name, override)

@Target(AnnotationTarget.CLASS)
annotation class Weak

fun <T> Binding<T>.asWeakBinding(): Binding<T> {
    if (this is UnlinkedWeakBinding) return this
    if (this is LinkedWeakBinding) return this
    return when (this) {
        is LinkedBinding -> LinkedWeakBinding(this)
        is UnlinkedBinding -> UnlinkedWeakBinding(this)
    }
}

private class UnlinkedWeakBinding<T>(private val binding: UnlinkedBinding<T>) :
    UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> =
        LinkedWeakBinding(binding.link(linker))
}

private class LinkedWeakBinding<T>(private val binding: LinkedBinding<T>) : LinkedBinding<T>() {
    private var _value: WeakReference<T>? = null
    override fun get(parameters: ParametersDefinition?): T =
        _value?.get() ?: binding(parameters).also { _value = WeakReference(it) }
}