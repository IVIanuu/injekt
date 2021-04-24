/*
 * Copyright 2021 Manuel Wrage
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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt

/**
 * Considers the annotated declaration in the current scope when resolving given arguments
 *
 * Example
 * ```
 * @Given val foo = Foo()
 * @Given fun bar(@Given foo: Foo) = Bar(foo)
 *
 * fun main() {
 *     val bar = given<Bar>()
 *     println("Got $bar")
 * }
 * ```
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER
)
annotation class Given

/**
 * Returns a given argument of type [T]
 */
inline fun <T> given(@Given value: T): T = value

/**
 * Returns a given argument of type [T] or null
 */
inline fun <T> givenOrNull(@Given @DefaultOnAllErrors value: T? = null): T? = value

/**
 * Marks an annotation as an qualifier which can then be used
 * to distinct types
 *
 * For example:
 * ```
 * @Qualifier
 * annotation class UserId
 *
 * @Qualifier
 * annotation class Username
 *
 * @Given
 * val userId: @UserId String = "123"
 *
 * @Given
 * val username: @Username String = "Foo"
 *
 * fun main() {
 *     val userId = given<@UserId String>()
 *     // userId = 123
 *     val username = given<@Username String>()
 *     // username = "Foo"
 * }
 * ```
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Qualifier

/**
 * Falls back to the default value if a given exists but has an error.
 * Normally the default value will only be used if no given was found but not if it has errors
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPE,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class DefaultOnAllErrors

/**
 * Only includes successful elements in the [Set] and ignores elements with errors
 */
@Target(AnnotationTarget.TYPE)
annotation class IgnoreElementsWithErrors
