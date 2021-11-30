/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

class ProvideLambdaTest {
  @Test fun testProvideLambda() = codegen(
    """
      fun invoke(foo: Foo) = inject<@Provide (@Provide () -> Foo) -> Foo>()({ foo })
    """
  ) {
    val foo = Foo()
    invokeSingleFile(foo) shouldBeSameInstanceAs foo
  }

  @Test fun testProvideLambdaChain() = singleAndMultiCodegen(
    """
      @Provide val fooModule: @Provide () -> @Provide () -> Foo = { { Foo() } }
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testProvideLambdaIdentity() = codegen(
    """
      private val foo1 = Foo()
      @Provide val foo1Lambda: @Provide () -> Foo = { foo1 }
      private val foo2 = Foo()
      @Provide val foo2Lambda: @Provide () -> Foo = { foo2 }
      fun invoke() = inject<List<Foo>>()
    """
  ) {
    val foos = invokeSingleFile<List<Foo>>()
    foos shouldBe foos.distinct()
  }
}
