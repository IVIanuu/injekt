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

inline fun <P1, R> provide(@Provide p1: P1, block: @Inject1<P1> () -> R): R = block()

inline fun <P1, P2, R> provide(
  @Provide p1: P1,
  @Provide p2: P2,
  block: @Inject2<P1, P2> () -> R
): R = block()

inline fun <P1, P2, P3, R> provide(
  @Provide p1: P1,
  @Provide p2: P2,
  @Provide p3: P3,
  block: @Inject3<P1, P2, P3> () -> R
): R = block()

inline fun <P1, P2, P3, P4, R> provide(
  @Provide p1: P1,
  @Provide p2: P2,
  @Provide p3: P3,
  @Provide p4: P4,
  block: @Inject4<P1, P2, P3, P4> () -> R
): R = block()

inline fun <P1, P2, P3, P4, P5, R> provide(
  @Provide p1: P1,
  @Provide p2: P2,
  @Provide p3: P3,
  @Provide p4: P4,
  @Provide p5: P5,
  block: @Inject5<P1, P2, P3, P4, P5> () -> R
): R = block()

inline fun <P1, P2, P3, P4, P5, P6, R> provide(
  @Provide p1: P1,
  @Provide p2: P2,
  @Provide p3: P3,
  @Provide p4: P4,
  @Provide p5: P5,
  @Provide p6: P6,
  block: @Inject6<P1, P2, P3, P4, P5, P6> () -> R
): R = block()

inline fun <P1, P2, P3, P4, P5, P6, P7, R> provide(
  @Provide p1: P1,
  @Provide p2: P2,
  @Provide p3: P3,
  @Provide p4: P4,
  @Provide p5: P5,
  @Provide p6: P6,
  @Provide p7: P7,
  block: @Inject7<P1, P2, P3, P4, P5, P6, P7> () -> R
): R = block()

inline fun <P1, P2, P3, P4, P5, P6, P7, P8, R> provide(
  @Provide p1: P1,
  @Provide p2: P2,
  @Provide p3: P3,
  @Provide p4: P4,
  @Provide p5: P5,
  @Provide p6: P6,
  @Provide p7: P7,
  @Provide p8: P8,
  block: @Inject8<P1, P2, P3, P4, P5, P6, P7, P8> () -> R
): R = block()


inline fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> provide(
  @Provide p1: P1,
  @Provide p2: P2,
  @Provide p3: P3,
  @Provide p4: P4,
  @Provide p5: P5,
  @Provide p6: P6,
  @Provide p7: P7,
  @Provide p8: P8,
  @Provide p9: P9,
  block: @Inject9<P1, P2, P3, P4, P5, P6, P7, P8, P9> () -> R
): R = block()

@Target(
  // fun func(@Inject foo: Foo)
  AnnotationTarget.VALUE_PARAMETER,

  // Lambda
  // val func: (@Inject Foo) -> Bar = { bar() }
  AnnotationTarget.TYPE
)
annotation class Inject

typealias Inject1<A> = Inject2<A, A>

@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.TYPE
)
@Repeatable
annotation class Inject2<out A, out B>

typealias Inject3<A, B, C> = Inject2<Inject2<A, B>, C>

typealias Inject4<A, B, C, D> = Inject2<Inject3<A, B, C>, D>

typealias Inject5<A, B, C, D, E> = Inject2<Inject4<A, B, C, D>, E>

typealias Inject6<A, B, C, D, E, F> = Inject2<Inject5<A, B, C, D, E>, F>

typealias Inject7<A, B, C, D, E, F, G> = Inject2<Inject6<A, B, C, D, E, F>, G>

typealias Inject8<A, B, C, D, E, F, G, H> = Inject2<Inject7<A, B, C, D, E, F, G>, H>

typealias Inject9<A, B, C, D, E, F, G, H, I> = Inject2<Inject8<A, B, C, D, E, F, G, H>, I>

/**
 * Returns a provided instance of [T]
 */
inline fun <T> inject(@Inject value: T): T = value

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
