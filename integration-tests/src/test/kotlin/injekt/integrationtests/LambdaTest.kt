/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package injekt.integrationtests

import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class LambdaTest {
  @Test fun testLambdaInjectable() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke(): Foo = create<() -> Foo>()()
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testLambdaInjectableWithParams() = codegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = create<(Foo) -> Bar>()(Foo()) 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testCannotRequestLambdaInjectableForNonExistingInjectable() = codegen(
    """ 
      fun invoke(): Foo = create<() -> Foo>()()
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }

  @Test fun testSuspendLambdaInjectable() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke(): Foo = runBlocking { create<suspend () -> Foo>()() }
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testComposableLambdaInjectable() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
    """,
    """
      fun invoke(): Foo = runComposing { create<@Composable () -> Foo>()() }
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }
}
