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

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt

/**
 * Considers the annotated declaration in the current scope when resolving given arguments
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
@Suppress("NOTHING_TO_INLINE")
inline fun <T> given(@Given value: T): T = value

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
