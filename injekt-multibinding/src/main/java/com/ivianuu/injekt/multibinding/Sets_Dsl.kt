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

package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.getOrSet
import com.ivianuu.injekt.withBinding
import kotlin.collections.set

/**
 * Declares a empty set binding with the [setQualifier]
 * This is useful for retrieving a [MultiBindingSet] even if no [Binding] was bound into it
 */
fun Module.setBinding(setQualifier: Qualifier) {
    factory(qualifier = setQualifier, override = true) {
        MultiBindingSet<Any>(component, emptySet())
    }
}

/**
 * Adds this [Binding] into [setBinding]
 */
infix fun <T> BindingContext<T>.bindIntoSet(setBinding: SetBinding): BindingContext<T> {
    binding.attributes.getOrSet(KEY_SET_BINDINGS) {
        hashMapOf<Qualifier, SetBinding>()
    }[setBinding.setQualifier] = setBinding

    module.declareSetBinding(setBinding.setQualifier)

    return this
}

/**
 * Adds this binding into [setQualifier]
 */
fun <T> BindingContext<T>.bindIntoSet(
    setQualifier: Qualifier,
    override: Boolean = false
): BindingContext<T> = bindIntoSet(SetBinding(setQualifier, override))

/**
 * Adds this [Binding] into [setQualifier]
 */
infix fun <T> BindingContext<T>.bindIntoSet(setQualifier: Qualifier): BindingContext<T> =
    bindIntoSet(SetBinding(setQualifier))

/**
 * Binds a already existing [Binding] into a [Set] named [setQualifier]
 */
inline fun <reified T> Module.bindIntoSet(
    setQualifier: Qualifier,
    override: Boolean = false,
    implementationQualifier: Qualifier? = null
) {
    bindIntoSet<T>(SetBinding(setQualifier, override), implementationQualifier)
}

/**
 * Binds a already existing [Binding] into [setBinding]
 */
inline fun <reified T> Module.bindIntoSet(
    setBinding: SetBinding,
    implementationQualifier: Qualifier? = null
) {
    withBinding<T>(implementationQualifier) {
        bindIntoSet(setBinding)
        binding.attributes[KEY_ORIGINAL_KEY] = Key(T::class, implementationQualifier)
    }
}