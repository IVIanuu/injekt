/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.nulls.*
import io.kotest.matchers.types.*
import org.junit.*

class ProviderTest {
  @Test fun testProviderInjectable() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke(): Foo = inject<() -> Foo>()()
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testProviderWithInjectableArgs() = codegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<(Foo) -> Bar>()(Foo()) 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testProviderWithTaggedInjectableArgs() = singleAndMultiCodegen(
    """
      @Tag annotation class MyTag
      @Provide fun bar(foo: @MyTag Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<(@MyTag Foo) -> Bar>()(Foo()) 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Bar>()
  }

  @Test fun testProviderModule() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
      class FooModule(@Provide val foo: Foo)
    """,
    """
      fun invoke() = inject<(FooModule) -> Bar>()(FooModule(Foo()))
    """
  )

  @Test fun testSuspendProviderInjectable() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
    """,
    """
      fun invoke(): Foo = runBlocking { inject<suspend () -> Foo>()() } 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testComposableProviderInjectable() = singleAndMultiCodegen(
    """
      @Provide val foo: Foo @Composable get() = Foo()
    """,
    """
      fun invoke() = runComposing { inject<@Composable () -> Foo>()() }
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testProviderWhichReturnsItsParameter() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<(Foo) -> Foo>()(Foo())
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testCannotRequestProviderForNonExistingInjectable() = codegen(
    """ 
      fun invoke(): Foo = inject<() -> Foo>()()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.Function0<com.ivianuu.injekt.test.Foo> for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testProviderWithNullableReturnTypeReturnsNullAsDefault() = codegen(
    """
      fun invoke() = inject<() -> Foo?>()()
    """
  ) {
    invokeSingleFile().shouldBeNull()
  }
}
