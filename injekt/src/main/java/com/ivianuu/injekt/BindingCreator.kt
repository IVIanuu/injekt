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

package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Used for code gen
 */
interface BindingCreator {
    fun <T> create(
        type: KClass<*>,
        definition: Definition<T>,
        args: Map<String, Any>
    ): Module

    val Map<String, Any>.name: String? get() = get("name") as? String
    val Map<String, Any>.scopeName: String? get() = get("scopeName") as? String
    val Map<String, Any>.override: Boolean get() = get("override") as? Boolean ?: false
    val Map<String, Any>.eager: Boolean get() = get("eager") as? Boolean ?: false

    fun String?.nullIfEmpty(): String? = if (isNullOrEmpty()) null else this
}

/**
 * Binding creator for factories
 */
class FactoryBindingCreator : BindingCreator {
    override fun <T> create(
        type: KClass<*>,
        definition: Definition<T>,
        args: Map<String, Any>
    ): Module {
        return module {
            factory(
                type, args.name.nullIfEmpty(), args.scopeName.nullIfEmpty(),
                args.override, definition
            )
        }
    }
}

/**
 * Binding creator for singles
 */
class SingleBindingCreator : BindingCreator {
    override fun <T> create(
        type: KClass<*>,
        definition: Definition<T>,
        args: Map<String, Any>
    ): Module {
        return module {
            single(
                type, args.name.nullIfEmpty(), args.scopeName.nullIfEmpty(),
                args.override, args.eager, definition
            )
        }
    }
}