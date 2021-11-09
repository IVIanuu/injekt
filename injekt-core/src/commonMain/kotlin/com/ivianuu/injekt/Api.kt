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
 * Provides this declaration in the current scope
 */
@Target(
  // @Provide class MyClass
  AnnotationTarget.CLASS,

  // class MyClass @Provide constructor()
  AnnotationTarget.CONSTRUCTOR,

  // @Provide myFunction(): Foo = ...
  AnnotationTarget.FUNCTION,

  // @Provide val myProperty: Foo get() = ...
  AnnotationTarget.PROPERTY,

  // @Provide val myVariable: Foo = ...
  AnnotationTarget.LOCAL_VARIABLE,

  // fun func(@Provide foo: Foo)
  AnnotationTarget.VALUE_PARAMETER,

  // Lambda
  // val func: (Foo) -> Bar = { foo: @Provide Foo -> bar() }
  AnnotationTarget.TYPE
)
annotation class Provide

/**
 * Automatically fills in a argument if no explicit argument was provided
 */
@Target(
  // fun func(@Inject foo: Foo)
  AnnotationTarget.VALUE_PARAMETER,

  // Lambda
  // val func: (@Inject Foo) -> Bar = { bar() }
  AnnotationTarget.TYPE
)
annotation class Inject

/**
 * Returns a provided instance of [T]
 */
inline fun <T> inject(@Inject x: T): T = x

/**
 * Imports injectables from the specified [importPaths] and use them when resolving injectables inside the declaration
 */
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.FILE,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.EXPRESSION
)
@Retention(AnnotationRetention.SOURCE)
annotation class Providers(vararg val importPaths: String)

/**
 * Marks an annotation as an tag which can then be used
 * to distinct types
 *
 * For example:
 * ```
 * @Tag annotation class UserId
 *
 * @Tag annotation class Username
 *
 * @Provide val userId: @UserId String = "123"
 *
 * @Provide val username: @Username String = "Foo"
 *
 * fun main() {
 *   val userId = inject<@UserId String>()
 *   // userId = 123
 *   val username = inject<@Username String>()
 *   // username = "Foo"
 * }
 * ```
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Tag

/**
 * Creates a version of the annotated injectable for each other injectable whose type matches the constraints
 * of the the annotated type parameter
 */
@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Spread

/**
 * Specify a custom error message if no injectable could be found
 */
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.ANNOTATION_CLASS,
  AnnotationTarget.TYPE,
  AnnotationTarget.VALUE_PARAMETER
)
annotation class InjectableNotFound(val message: String)

/**
 * Specify a custom error message if ambiguous injectables were found
 */
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.TYPE,
  AnnotationTarget.LOCAL_VARIABLE
)
annotation class AmbiguousInjectable(val message: String)
