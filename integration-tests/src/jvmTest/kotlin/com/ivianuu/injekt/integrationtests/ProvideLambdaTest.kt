/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import androidx.compose.runtime.*
import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import io.kotest.matchers.types.*
import kotlinx.coroutines.*
import org.junit.*

class ProvideLambdaTest {
  @Test fun testProvideLambda() = codegen(
    """
      fun invoke() = inject<@Provide (@Provide () -> Foo) -> Foo>()
    """
  ) {
    val foo = Foo()
    invokeSingleFile<(() -> Foo) -> Foo>()({ foo }) shouldBeSameInstanceAs foo
  }

  @Test fun testSuspendProvideLambda() = codegen(
    """
      fun invoke() = inject<@Provide suspend (@Provide suspend () -> Foo) -> Foo>()
    """
  ) {
    runBlocking {
      val foo = Foo()
      invokeSingleFile<suspend (suspend () -> Foo) -> Foo>()({ foo }) shouldBeSameInstanceAs foo
    }
  }

  @Test fun testComposableProvideLambda() = codegen(
    """
      fun invoke() = inject<@Provide @Composable (@Provide @Composable () -> Foo) -> Foo>()
    """
  ) {
    runComposing {
      val foo = Foo()
      invokeSingleFile<@Composable (@Composable () -> Foo) -> Foo>()({ foo }) shouldBeSameInstanceAs foo
    }
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
