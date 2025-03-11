/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NOTHING_TO_INLINE")

package injekt

import kotlin.annotation.AnnotationTarget.*

/**
 * Provides this declaration in the current scope
 */
@Target(
  // @Provide class MyClass
  CLASS,

  // class MyClass @Provide constructor()
  CONSTRUCTOR,

  // @Provide myFunction(): Foo = ...
  FUNCTION,

  // @Provide val myProperty: Foo get() = ...
  PROPERTY,

  // @Provide val myVariable: Foo = ...
  LOCAL_VARIABLE,

  // fun func(@Provide foo: Foo)
  VALUE_PARAMETER,

  // Lambda
  // val func: (Foo) -> Bar = { foo: @Provide Foo -> bar() }
  TYPE
)
annotation class Provide

/**
 * Marks the parameter as injectable if used as default value
 * If no explicit parameter is passed injekt will fill in the parameter at each call site
 * see [create]
 */
val inject: Nothing = throw IllegalStateException("injekt compiler intrinsic")

/**
 * Creates a new instance of [T] using the enclosing providers
 */
inline fun <T> create(x: T = inject): T = x

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
 *   val userId = create<@UserId String>()
 *   // userId = 123
 *   val username = create<@Username String>()
 *   // username = "Foo"
 * }
 * ```
 */
@Target(ANNOTATION_CLASS, TYPEALIAS)
annotation class Tag

/**
 * Todo
 */
@Target(TYPE_PARAMETER)
annotation class AddOn
