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

inline fun <reified T> ComponentBuilder.instance(
    instance: T,
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
): BindingContext<T> = instance(
    instance = instance,
    key = keyOf(qualifier = qualifier),
    duplicateStrategy = duplicateStrategy
)

/**
 * Adds the [instance] as a binding for [key]
 */
fun <T> ComponentBuilder.instance(
    instance: T,
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
): BindingContext<T> = bind(
    key = key,
    behavior = behavior,
    duplicateStrategy = duplicateStrategy,
    provider = { instance }
)
