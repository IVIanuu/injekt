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

inline fun <reified S, reified T> ComponentBuilder.alias(
    originalQualifiers: Qualifier = Qualifier.None,
    aliasQualifiers: Qualifier = Qualifier.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
): BindingContext<T> = alias<S, T>(
    originalKey = keyOf(qualifier = originalQualifiers),
    aliasKey = keyOf(qualifier = aliasQualifiers),
    duplicateStrategy = duplicateStrategy
)

/**
 * Makes the [Binding] for [originalKey] retrievable via [aliasKey]
 *
 * For example the following code points the Repository request to RepositoryImpl
 *
 * ´´´
 * val component = Component {
 *     factory { RepositoryImpl() }
 *     alias<RepositoryImpl, Repository>()
 * }
 *
 * val repository = component.get<Repository>()
 *
 * ´´´
 *
 */
fun <S, T> ComponentBuilder.alias(
    originalKey: Key<S>,
    aliasKey: Key<T>,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
): BindingContext<T> = factory(
    key = aliasKey,
    duplicateStrategy = duplicateStrategy
) { parameters -> get(originalKey, parameters = parameters) as T }