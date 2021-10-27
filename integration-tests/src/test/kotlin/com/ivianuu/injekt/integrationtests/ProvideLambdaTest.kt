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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldNotContain
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

  @Test fun testCanRequestProvideLambda() = singleAndMultiCodegen(
    """
      typealias MyAlias = @Composable () -> Unit
      @Provide val myAlias: MyAlias = {}
      @Provide class MyComposeView(val content: @Composable () -> Unit)
    """,
    """
      fun invoke() = inject<(@Provide @Composable () -> Unit) -> MyComposeView>()
    """
  ) {
    irShouldNotContain("content = <get-myAlias>()")
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
