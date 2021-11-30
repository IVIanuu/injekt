/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import org.junit.*

class ExpressionWrappingTest {
  @Test fun testDoesFunctionWrapInjectableWithMultipleUsages() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<Bar, Bar>>()
    """
  ) {
    irShouldContain(1, "bar(foo = ")
  }

  @Test fun testDoesFunctionWrapInjectableWithMultipleUsagesInDifferentScopes() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
      @Provide fun <T> pair(a: T, b: () -> T): Pair<T, () -> T> = a to b
    """,
    """
      fun invoke() = inject<Pair<Bar, () -> Bar>>()
    """
  ) {
    irShouldContain(1, "bar(foo = ")
  }

  @Test fun testDoesNotFunctionWrapInjectableWithSingleUsage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<Bar>()
    """
  ) {
    irShouldNotContain("local fun <anonymous>(): Bar {")
  }

  @Test fun testDoesNotWrapInjectableWithMultipleUsagesButWithoutDependencies() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<Foo, Foo>>()
    """
  ) {
    irShouldNotContain("local fun <anonymous>(): Foo {")
  }

  @Test fun testSearchBetterName() = codegen(
    """
      interface Logger

      @Provide object AndroidLogger : Logger

      @Provide fun androidLogger(factory: () -> AndroidLogger): Logger = factory()

      @Provide data class MyComponent(
        val loggerFactory: () -> Logger,
        val loggerFactory2: () -> Logger
      )

      fun invoke() {
        inject<MyComponent>()
      }
    """
  )

  @Test fun testDoesFunctionWrapListInjectablesWithSameElements() = singleAndMultiCodegen(
    """
      @Provide val a = "a"
      @Provide val b = "b"
      @Provide fun <T> pair(a: T, b: () -> T): Pair<T, () -> T> = a to b
    """,
    """
      fun invoke() = inject<Pair<List<String>, () -> List<String>>>()
    """
  ) {
    irShouldContain(1, "local fun function0(): List<String> {")
  }

  @Test fun testDoesFunctionWrapProviderWithMultipleUsages() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<() -> Foo, () -> Foo>>()
    """
  ) {
    irShouldContain(1, "local fun function0(): Function0<Foo>")
  }

  @Test fun testDoesNotFunctionWrapProviderWithSingleUsage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<() -> Foo>()
    """
  ) {
    irShouldNotContain("local fun function0(): Function0<Foo>")
  }

  @Test fun testDoesNotFunctionWrapInlineProvider() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide inline fun bar(fooProvider: () -> Foo) = Bar(fooProvider())
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = inject<Pair<Bar, Bar>>()
    """
  ) {
    irShouldNotContain("local fun function0(): Function0<Foo>")
  }

  @Test fun testDoesNotFunctionWrapCircularDependency() = singleAndMultiCodegen(
    """
      @Provide class A(b: B)
      @Provide class B(a: () -> A, a2: () -> A)
     """,
    """
      fun invoke() = inject<B>() 
    """
  ) {
    irShouldNotContain("local fun function0(): Function0<Foo>")
    invokeSingleFile()
  }
}
