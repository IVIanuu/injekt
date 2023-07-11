/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
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

inline fun <F : Function<*>> provider(f: F): @Provide F = f

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
 * Returns a provided instance of [A]
 */
inline fun <A> inject(@Inject a: A): A = a

inline fun <A, R> inject(@Inject a: A, block: context (A) () -> R) = block(a)

inline fun <A, B, R> inject(@Inject a: A, @Inject b: B, block: context (A, B) () -> R) = block(a, b)

inline fun <A, B, C, R> inject(
  @Inject a: A,
  @Inject b: B,
  @Inject c: C,
  block: context (A, B, C) () -> R,
) =
  block(a, b, c)

inline fun <A, B, C, D, R> inject(
  @Inject a: A,
  @Inject b: B,
  @Inject c: C,
  @Inject d: D,
  block: context (A, B, C, D) () -> R,
) = block(a, b, c, d)

inline fun <A, B, C, D, E, R> inject(
  @Inject a: A,
  @Inject b: B,
  @Inject c: C,
  @Inject d: D,
  @Inject e: E,
  block: context (A, B, C, D, E) () -> R,
) = block(a, b, c, d, e)

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
