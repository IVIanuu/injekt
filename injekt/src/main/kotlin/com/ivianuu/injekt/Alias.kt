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

inline fun <reified S : T, reified T> ComponentBuilder.alias(
    originalQualifier: Qualifier = Qualifier.None,
    aliasQualifier: Qualifier = Qualifier.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) = alias<S, T>(
    originalKey = keyOf(qualifier = originalQualifier),
    aliasKey = keyOf(qualifier = aliasQualifier),
    duplicateStrategy = duplicateStrategy
)

/**
 * Makes the [Binding] for [originalKey] retrievable via [aliasKey]
 *
 * For example the following code points the Repository request to RepositoryImpl
 *
 * ´´´
 * val component = Component {
 *     single { RepositoryImpl() }
 *     alias<RepositoryImpl, Repository>()
 * }
 *
 * val repositoryA = component.get<RepositoryImpl>()
 * val repositoryB = component.get<Repository>()
 * assertSame(repositoryA, repositoryB) // true
 *
 * ´´´
 *
 */
fun <S, T> ComponentBuilder.alias(
    originalKey: Key<S>,
    aliasKey: Key<T>,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) = bind(
    key = aliasKey,
    duplicateStrategy = duplicateStrategy
) { parameters -> get(originalKey, parameters = parameters) as T }

inline fun <reified T> ComponentBuilder.alias(
    aliasQualifier: Qualifier,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) = alias<T, T>(aliasQualifier = aliasQualifier, duplicateStrategy = duplicateStrategy)

fun <T> ComponentBuilder.alias(
    originalKey: Key<T>,
    aliasQualifier: Qualifier,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) = alias(
    originalKey = originalKey,
    aliasKey = originalKey.copy(qualifier = aliasQualifier),
    duplicateStrategy = duplicateStrategy
)