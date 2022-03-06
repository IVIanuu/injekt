/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
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

  @Test fun testProviderWithParameters() = codegen(
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

  @Test fun testCannotRequestProviderForNonExistingInjectable() = codegen(
    """ 
      fun invoke(): Foo = inject<() -> Foo>()()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.Function0<com.ivianuu.injekt.test.Foo> for parameter x of function com.ivianuu.injekt.inject")
  }
}
