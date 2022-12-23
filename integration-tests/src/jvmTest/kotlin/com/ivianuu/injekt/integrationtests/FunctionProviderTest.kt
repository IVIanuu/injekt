/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.withCompose
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

class FunctionProviderTest {
  @Test fun testFunctionProvider() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke(): Foo = context<() -> Foo>()()
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testFunctionProviderWithProviderArgs() = codegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = context<(Foo) -> Bar>()(Foo()) 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testFunctionProviderWithTaggedProviderArgs() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag
      @Provide fun bar(foo: @MyTag Foo) = Bar(foo)
    """,
    """
      fun invoke() = context<(@MyTag Foo) -> Bar>()(Foo()) 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testFunctionProviderModule() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
      class FooModule(@Provide val foo: Foo)
    """,
    """
      fun invoke() = context<(FooModule) -> Bar>()(FooModule(Foo()))
    """
  )

  @Test fun testSuspendFunctionProvider() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
    """,
    """
      fun invoke(): Foo = runBlocking { context<suspend () -> Foo>()() } 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testComposableFunctionProvider() = singleAndMultiCodegen(
    """
      @Provide val foo: Foo @Composable get() = Foo()
    """,
    """
      fun invoke() = runComposing { context<@Composable () -> Foo>()() }
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testFunctionProviderWhichReturnsItsParameter() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = context<(Foo) -> Foo>()(Foo())
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testCannotRequestFunctionProviderForNonExistingProvider() = codegen(
    """ 
      fun invoke(): Foo = context<() -> Foo>()()
    """
  ) {
    compilationShouldHaveFailed("no provider found of type kotlin.Function0<com.ivianuu.injekt.test.Foo> for parameter x of function com.ivianuu.injekt.context")
  }
}
