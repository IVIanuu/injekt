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

inline fun <reified T> ComponentBuilder.factory(
    qualifier: Qualifier = Qualifier.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    bound: Boolean = false,
    noinline provider: BindingProvider<T>
): BindingContext<T> = factory(
    key = keyOf(qualifier = qualifier),
    duplicateStrategy = duplicateStrategy,
    bound = bound,
    provider = provider
)

/**
 * Adds a binding for [key] which will be instantiated on each request
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
 * assertEquals(logger1, logger2) // false

 *
 * ´´´
 *
 * @param key the key to retrieve the instance
 * @param duplicateStrategy the strategy for handling overrides
 * @param bound whether instances should be created in the scope of the component
 * @param provider the definitions which creates instances
 *
 * @see ComponentBuilder.add
 */
fun <T> ComponentBuilder.factory(
    key: Key<T>,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    bound: Boolean = false,
    provider: BindingProvider<T>
): BindingContext<T> = add(
    Binding(
        key = key,
        behavior = if (bound) BoundBehavior() else Behavior.None,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
)

/**
 * Makes the annotated class injectable and generates a factory binding for it
 * The class will be created on each request
 *
 * @see Single
 * @see Qualifier
 * @see ScopeMarker
 * @see InjektConstructor
 * @see ComponentBuilder.factory
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class Factory

