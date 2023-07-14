/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NOTHING_TO_INLINE")

package com.ivianuu.injekt

import com.ivianuu.injekt.Providers.Empty

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
  AnnotationTarget.TYPE,

  AnnotationTarget.PROPERTY
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

inline fun <A, R> provide(@Provide a: A, block: (@Provide Providers1<A>) -> R): R = block(inject())

inline fun <A, B, R> provide(@Provide a: A, @Provide b: B, block: (@Provide Providers2<A, B>) -> R): R =
  block(inject())

inline fun <A, B, C, R> provide(
  @Provide a: A,
  @Provide b: B,
  @Provide c: C,
  block: (@Provide Providers3<A, B, C>) -> R
): R = block(inject())

inline fun <A, B, C, D, R> provide(
  @Provide a: A,
  @Provide b: B,
  @Provide c: C,
  @Provide d: D,
  block: (@Provide Providers4<A, B, C, D>) -> R
): R = block(inject())

inline fun <A, B, C, D, E, R> provide(
  @Provide a: A,
  @Provide b: B,
  @Provide c: C,
  @Provide d: D,
  @Provide e: E,
  block: (@Provide Providers5<A, B, C, D, E>) -> R
): R = block(inject())

inline fun <A, B, C, D, E, F, R> provide(
  @Provide a: A,
  @Provide b: B,
  @Provide c: C,
  @Provide d: D,
  @Provide e: E,
  @Provide f: F,
  block: (@Provide Providers6<A, B, C, D, E, F>) -> R
): R = block(inject())

inline fun <A, B, C, D, E, F, G, R> provide(
  @Provide a: A,
  @Provide b: B,
  @Provide c: C,
  @Provide d: D,
  @Provide e: E,
  @Provide f: F,
  @Provide g: G,
  block: (@Provide Providers7<A, B, C, D, E, F, G>) -> R
): R = block(inject())

inline fun <A, B, C, D, E, F, G, H, R> provide(
  @Provide a: A,
  @Provide b: B,
  @Provide c: C,
  @Provide d: D,
  @Provide e: E,
  @Provide f: F,
  @Provide g: G,
  @Provide h: H,
  block: (@Provide Providers8<A, B, C, D, E, F, G, H>) -> R
): R = block(inject())

inline fun <A, B, C, D, E, F, G, H, I, R> provide(
  @Provide a: A,
  @Provide b: B,
  @Provide c: C,
  @Provide d: D,
  @Provide e: E,
  @Provide f: F,
  @Provide g: G,
  @Provide h: H,
  @Provide i: I,
  block: (@Provide Providers9<A, B, C, D, E, F, G, H, I>) -> R
): R = block(inject())

typealias Providers1<A> = Providers<A, Empty, Empty, Empty, Empty, Empty, Empty, Empty, Empty>
typealias Providers2<A, B> = Providers<A, B, Empty, Empty, Empty, Empty, Empty, Empty, Empty>
typealias Providers3<A, B, C> = Providers<A, B, C, Empty, Empty, Empty, Empty, Empty, Empty>
typealias Providers4<A, B, C, D> = Providers<A, B, C, D, Empty, Empty, Empty, Empty, Empty>
typealias Providers5<A, B, C, D, E> = Providers<A, B, C, D, E, Empty, Empty, Empty, Empty>
typealias Providers6<A, B, C, D, E, F> = Providers<A, B, C, D, E, F, Empty, Empty, Empty>
typealias Providers7<A, B, C, D, E, F, G> = Providers<A, B, C, D, E, F, G, Empty, Empty>
typealias Providers8<A, B, C, D, E, F, G, H> = Providers<A, B, C, D, E, F, G, H, Empty>
typealias Providers9<A, B, C, D, E, F, G, H, I> = Providers<A, B, C, D, E, F, G, H, I>

@Provide data class Providers<A, B, C, D, E, F, G, H, I>(
  @Inject @property:Provide val a: A,
  @Inject @property:Provide val b: B,
  @Inject @property:Provide val c: C,
  @Inject @property:Provide val d: D,
  @Inject @property:Provide val e: E,
  @Inject @property:Provide val f: F,
  @Inject @property:Provide val g: G,
  @Inject @property:Provide val h: H,
  @Inject @property:Provide val i: I
) {
  @Provide object Empty
}
