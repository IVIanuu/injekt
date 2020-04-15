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
annotation class Factory {
    companion object : Behavior.Element
}

inline fun <reified T> ComponentBuilder.factory(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
) {
    factory(
        key = keyOf(qualifier),
        behavior = behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}

fun <T> ComponentBuilder.factory(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    provider: BindingProvider<T>
) {
    bind(
        key = key,
        behavior = Factory + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
}