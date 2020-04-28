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

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

/**
 * Creates instances on each request
 *
 * We get different logger instances in the following example
 *
 * ´´´
 * val component = Component {
 *     factory { Logger(get()) }
 * }
 *
 * val logger1 = component.get<Logger>()
 * val logger2 = component.get<Logger>()
 * assertSame(logger1, logger2) // false
 * ´´´
 */
@Scope
annotation class Factory

inline fun <reified T> ComponentDsl.factory(
    qualifier: KClass<*>? = null,
    bindingDefinition: BindingDefinition<T>
): Unit = injektIntrinsic()

inline fun <reified T> ComponentDsl.factory(
    qualifier: KClass<*>? = null,
    binding: Binding<T>
): Unit = injektIntrinsic()

fun <T> ComponentDsl.factory(
    key: Key<T>,
    bindingDefinition: BindingDefinition<T>
): Unit = injektIntrinsic()

fun <T> ComponentDsl.factory(
    key: Key<T>,
    binding: Binding<T>
) {
    add(key, binding)
}
