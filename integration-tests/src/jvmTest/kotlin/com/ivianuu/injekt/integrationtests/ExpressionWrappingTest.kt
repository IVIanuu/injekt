/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.irShouldNotContain
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.junit.Test

class ExpressionWrappingTest {
  @Test fun testDoesFunctionWrapProviderWithMultipleUsages() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = context<Pair<Bar, Bar>>()
    """
  ) {
    irShouldContain(1, "bar(foo = ")
  }

  @Test fun testDoesFunctionWrapProviderWithMultipleUsagesInDifferentScopes() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
      @Provide fun <T> pair(a: T, b: () -> T): Pair<T, () -> T> = a to b
    """,
    """
      fun invoke() = context<Pair<Bar, () -> Bar>>()
    """
  ) {
    irShouldContain(1, "bar(foo = ")
  }

  @Test fun testDoesNotFunctionWrapProviderWithSingleUsage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = context<Bar>()
    """
  ) {
    irShouldNotContain("local fun <anonymous>(): Bar {")
  }

  @Test fun testDoesNotWrapProviderWithMultipleUsagesButWithoutDependencies() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = context<Pair<Foo, Foo>>()
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
        context<MyComponent>()
      }
    """
  )

  @Test fun testDoesFunctionWrapListProvidersWithSameElements() = singleAndMultiCodegen(
    """
      @Provide val a = "a"
      @Provide val b = "b"
      @Provide fun <T> pair(a: T, b: () -> T): Pair<T, () -> T> = a to b
    """,
    """
      fun invoke() = context<Pair<List<String>, () -> List<String>>>()
    """
  ) {
    irShouldContain(1, "local fun function0(): List<String> {")
  }

  @Test fun testDoesFunctionWrapFunctionProviderWithMultipleUsages() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = context<Pair<() -> Foo, () -> Foo>>()
    """
  ) {
    irShouldContain(1, "local fun function0(): Function0<Foo>")
  }

  @Test fun testDoesNotFunctionWrapFunctionProviderWithSingleUsage() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = context<() -> Foo>()
    """
  ) {
    irShouldNotContain("local fun function0(): Function0<Foo>")
  }

  @Test fun testDoesNotFunctionWrapInlineFunctionProvider() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide inline fun bar(fooProvider: () -> Foo) = Bar(fooProvider())
      @Provide fun <T> pair(a: T, b: T): Pair<T, T> = a to b
    """,
    """
      fun invoke() = context<Pair<Bar, Bar>>()
    """
  ) {
    irShouldNotContain("local fun function0(): Function0<Foo>")
  }
}
