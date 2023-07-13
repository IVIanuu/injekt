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

inline fun <A, R> provide(
  a: A,
  block: context((@Provide A)) () -> R
): R = block(a)

inline fun <A, B, R> provide(
  a: A,
  b: B,
  block: context((@Provide A), (@Provide B)) () -> R
): R = block(a, b)

inline fun <A, B, C, R> provide(
  a: A,
  b: B,
  c: C,
  block: context((@Provide A), (@Provide B), (@Provide C)) () -> R
): R = block(a, b, c)

inline fun <A, B, C, D, R> provide(
  a: A,
  b: B,
  c: C,
  d: D,
  block: context((@Provide A), (@Provide B), (@Provide C), (@Provide D)) () -> R
): R = block(a, b, c, d)

inline fun <A, B, C, D, E, R> provide(
  a: A,
  b: B,
  c: C,
  d: D,
  e: E,
  block: context((@Provide A), (@Provide B), (@Provide C), (@Provide D), (@Provide E)) () -> R
): R = block(a, b, c, d, e)

context((T)) inline fun <T> context(): T = this@T

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
 *   val userId = context<@UserId String>()
 *   // userId = 123
 *   val username = context<@Username String>()
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
