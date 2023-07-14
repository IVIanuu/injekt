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

inline fun <A, R> provide(a: A, block: (@Provide A) -> R): R = block(a)

data class ProvideModule2<A, B>(
  @property:Provide val _a: A,
  @property:Provide val _b: B
)

inline fun <A, B, R> provide(a: A, b: B, block: (@Provide ProvideModule2<A, B>) -> R): R =
  block(ProvideModule2(a, b))

data class ProvideModule3<A, B, C>(
  @property:Provide val _a: A,
  @property:Provide val _b: B,
  @property:Provide val _c: C
)

inline fun <A, B, C, R> provide(
  a: A,
  b: B,
  c: C,
  block: (@Provide ProvideModule3<A, B, C>) -> R
): R = block(ProvideModule3(a, b, c))

data class ProvideModule4<A, B, C, D>(
  @property:Provide val _a: A,
  @property:Provide val _b: B,
  @property:Provide val _c: C,
  @property:Provide val _D: D
)

inline fun <A, B, C, D, R> provide(
  a: A,
  b: B,
  c: C,
  d: D,
  block: (@Provide ProvideModule4<A, B, C, D>) -> R
): R = block(ProvideModule4(a, b, c, d))

data class ProvideModule5<A, B, C, D, E>(
  @property:Provide val _a: A,
  @property:Provide val _b: B,
  @property:Provide val _c: C,
  @property:Provide val _D: D,
  @property:Provide val _e: E
)

inline fun <A, B, C, D, E, R> provide(
  a: A,
  b: B,
  c: C,
  d: D,
  e: E,
  block: (@Provide ProvideModule5<A, B, C, D, E>) -> R
): R = block(ProvideModule5(a, b, c, d, e))

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
