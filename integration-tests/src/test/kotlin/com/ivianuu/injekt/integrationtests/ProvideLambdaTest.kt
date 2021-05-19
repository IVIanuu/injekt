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

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.junit.*

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
      @Provide fun myAlias(): MyAlias = {}
      @Provide class MyComposeView(val content: @Composable () -> Unit)
    """,
    """
      fun invoke() = inject<(@Provide @Composable () -> Unit) -> MyComposeView>() 
    """
  )
}
