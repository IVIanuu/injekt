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

package com.ivianuu.injekt.eager

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.LinkedSingleBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.ModuleBuilder
import com.ivianuu.injekt.ParametersDefinition
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.UnlinkedBinding
import com.ivianuu.injekt.UnlinkedDefinitionBinding
import com.ivianuu.injekt.UnlinkedSingleBinding
import com.ivianuu.injekt.add
import com.ivianuu.injekt.asSingleBinding
import com.ivianuu.injekt.typeOf

inline fun <reified T> ModuleBuilder.eager(
    name: Qualifier? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = eager(typeOf(), name, override, definition)

fun <T> ModuleBuilder.eager(
    type: Type<T>,
    name: Qualifier? = null,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> =
    add(UnlinkedDefinitionBinding(definition).asEagerBinding(), type, name, override)


fun <T> Binding<T>.asEagerBinding(): Binding<T> {
    if (this is UnlinkedEagerBinding) return this
    if (this is LinkedEagerBinding) return this
    return when (this) {
        is LinkedBinding -> LinkedEagerBinding(
            if (this is LinkedSingleBinding) this
            else asSingleBinding() as LinkedSingleBinding
        )
        is UnlinkedBinding -> UnlinkedEagerBinding(
            if (this is UnlinkedSingleBinding) this
            else asSingleBinding() as UnlinkedSingleBinding
        )
    }
}

private class UnlinkedEagerBinding<T>(private val binding: UnlinkedSingleBinding<T>) :
    UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> =
        LinkedEagerBinding(binding.link(linker) as LinkedSingleBinding<T>)
}

private class LinkedEagerBinding<T>(private val binding: LinkedSingleBinding<T>) :
    LinkedBinding<T>() {
    override fun get(parameters: ParametersDefinition?): T = binding.get(parameters)
}