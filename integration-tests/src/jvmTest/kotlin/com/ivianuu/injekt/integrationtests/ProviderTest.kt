/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.withCompose
import io.kotest.matchers.types.shouldBeTypeOf
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

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
}
