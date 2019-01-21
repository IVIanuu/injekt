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
import com.ivianuu.injekt.OverrideException
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.getOrDefault
import com.ivianuu.injekt.getOrSet
import java.util.*
import kotlin.reflect.KClass

/**
 * Declares a empty set binding with the [setName]
 * This is useful for retrieving a [MultiBindingSet] even if no [Binding] was bound into it
 */
fun Module.setBinding(setBinding: SetBinding) {
    factory(name = setBinding.setName, override = true) {
        MultiBindingSet<Any>(component, emptySet())
    }
}

/**
 * Binds this [Binding] into [setBinding]
 */
infix fun <T> BindingContext<T>.bindIntoSet(setBinding: SetBinding): BindingContext<T> {
    binding.attributes.getOrSet(KEY_SET_BINDINGS) {
        mutableMapOf<String, SetBinding>()
    }[setBinding.setName] = setBinding

    module.factory(name = setBinding.setName, override = true) {
        val allSetBindings = component.getAllBindings()
            .mapNotNull { binding ->
                binding.attributes.get<Map<String, SetBinding>>(KEY_SET_BINDINGS)
                    ?.get(setBinding.setName)?.let { binding to it }
            }

        val existingKeys = mutableSetOf<Key>()

        // check overrides
        allSetBindings.forEach { (binding, setBinding) ->
            val key = binding.attributes.getOrDefault(KEY_ORIGINAL_KEY) { binding.key }
            if (!existingKeys.add(key) && !setBinding.override) {
                throw OverrideException("Try to override $key in set binding $setBinding")
            }
        }

        allSetBindings
            .map { it.first as Binding<Any> }
            .toSet()
            .let { MultiBindingSet(component, it) }
    }

    return this
}

/**
 * Binds a already existing [Binding] into a [Set] named [setName]
 */
inline fun <reified T> Module.bindIntoSet(
    setBinding: SetBinding,
    implementationName: String? = null
) {
    bindIntoSet<T>(T::class, setBinding, implementationName)
}

/**
 * Binds a already existing [Binding] into a [Set] named [setName]
 */
fun <T> Module.bindIntoSet(
    implementationType: KClass<*>,
    setBinding: SetBinding,
    implementationName: String? = null
) {
    // we use a unique id here to make sure that the binding does not collide with any user config
    val context = factory(implementationType, UUID.randomUUID().toString()) {
        get<T>(implementationType, implementationName) { it }
    } bindIntoSet setBinding

    context.binding.attributes[KEY_ORIGINAL_KEY] = Key(implementationType, implementationName)
}