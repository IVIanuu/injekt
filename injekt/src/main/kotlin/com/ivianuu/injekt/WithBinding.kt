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

import java.util.UUID

inline fun <reified T> ComponentBuilder.withBinding(
    qualifier: Qualifier = Qualifier.None,
    noinline block: BindingContext<T>.() -> Unit
) {
    withBinding(key = keyOf(qualifier = qualifier), block = block)
}

/**
 * Runs the [block] in the [BindingContext] of the [Binding] for [key]
 * This allows to add aliases to bindings which are declared somewhere else
 *
 * For example to add a alias for a annotated class one can write the following:
 *
 * ´@Factory class MyRepository : Repository`
 *
 * ´´´
 * withBinding(key = keyOf<MyRepository>()) {
 *     bindAlias<Repository>()
 * }
 *
 * ´´´
 *
 */
fun <T> ComponentBuilder.withBinding(
    key: Key<T>,
    block: BindingContext<T>.() -> Unit
) {
    // we create a alias of the original binding with a UUID qualifier
    // because we have no reference to the original one it's likely in another [Component]
    // we use a unique id here to make sure that the binding does not collide with any user config
    alias(
        originalKey = key,
        aliasKey = key.copy(qualifier = UUIDQualifier())
    ).block()
}

private data class UUIDQualifier(private val uuid: UUID = UUID.randomUUID()) : Qualifier.Element
