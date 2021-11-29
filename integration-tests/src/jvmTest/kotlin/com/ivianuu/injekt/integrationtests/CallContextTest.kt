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

import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.withCompose
import org.junit.Test

class CallContextTest {
  @Test fun testSuspendCannotBeRequestedFromNonSuspend() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
      @Provide suspend fun bar(foo: Foo) = Bar(foo)
    """,
    """
      @Composable fun invoke() = inject<Bar>()
    """,
    config = { withCompose() }
  ) {
    compilationShouldHaveFailed("injectable com.ivianuu.injekt.integrationtests.bar() of type com.ivianuu.injekt.test.Bar for parameter x of function com.ivianuu.injekt.inject is a suspend function but current call context is composable")
  }

  @Test fun testComposableDependencyCannotBeRequestedFromNonComposable() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
      @Provide @Composable fun bar(foo: Foo) = Bar(foo)
    """,
    """
      suspend fun invoke() = inject<Bar>()
    """,
    config = { withCompose() }
  ) {
    compilationShouldHaveFailed("injectable com.ivianuu.injekt.integrationtests.bar() of type com.ivianuu.injekt.test.Bar for parameter x of function com.ivianuu.injekt.inject is a composable function but current call context is suspend")
  }

  @Test fun testNonSuspendInjectableCanReceiveSuspendInjectableInSuspendContext() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = runBlocking { inject<Bar>() } 
    """
  )

  @Test fun testNonComposableInjectableCanReceiveComposableInjectableInComposableContext() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      @Composable fun invoke() { inject<Bar>() } 
    """
  )

  @Test fun testSuspendProviderCanRequestSuspendDependencyInDefaultContext() =
    singleAndMultiCodegen(
      """
        @Provide suspend fun foo() = Foo()
        @Provide fun bar(foo: Foo) = Bar(foo)
      """,
      """
        fun invoke() = inject<suspend () -> Bar>()
      """
    )

  @Test fun testComposableProviderCanRequestComposableDependencyInDefaultContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
        @Provide fun bar(foo: Foo) = Bar(foo)
      """,
      """
        fun invoke() = inject<@Composable () -> Bar>()
      """
    )
  
  @Test fun testCanRequestSuspendDependencyFromNonSuspendFunctionInSuspendLambda() =
    singleAndMultiCodegen(
      """
        @Provide suspend fun foo() = Foo()
        @Provide fun lazyBar(): suspend () -> Bar = { Bar(inject()) }
      """,
      """
        fun invoke() = inject<suspend () -> Bar>()
      """
    )

  @Test fun testCanRequestComposableDependencyFromNonComposableFunctionInComposableLambda() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
        @Provide fun lazyBar(): @Composable () -> Bar = { Bar(inject()) }
      """,
      """
        fun invoke() = inject<@Composable () -> Bar>()
      """,
      config = { withCompose() }
    )

  @Test fun testSuspendCanBeRequestedFromInlineLambdaInSuspendContext() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
    """,
    """
      fun invoke() = runBlocking {
        run {
          run {
            inject<Foo>()
          }
        }
      } 
    """
  )

  @Test fun testComposableCanBeRequestedFromInlineLambdaInComposableContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        @Composable fun invoke() = run {
          run {
            inject<Foo>()
          }
        }
      """,
      config = { withCompose() }
    )

  @Test fun testSuspendCanBeRequestedFromLocalVariableInitializerInSuspendContext() =
    singleAndMultiCodegen(
      """
        @Provide suspend fun foo() = Foo()
      """,
      """
        suspend fun invoke() {
          val foo = inject<Foo>()
        }
      """
    )

  @Test fun testComposableCanBeRequestedFromLocalVariableInitializerInComposableContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        @Composable fun invoke() {
          val foo = inject<Foo>()
        }
      """,
      config = { withCompose() }
    )

  @Test fun testSuspendCanBeRequestedFromLocalVariableDelegateInitializerInSuspendContext() =
    singleAndMultiCodegen(
      """
        @Provide suspend fun foo() = Foo()
      """,
      """
        suspend fun invoke() {
          val foo = lazy(inject<Foo>()) {  }
        }
      """
    )

  @Test fun testComposableCanBeRequestedFromLocalVariableDelegateInitializerInComposableContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        @Composable fun invoke() {
          val foo = lazy(inject<Foo>()) {  }
        }
      """,
      config = { withCompose() }
    )

  @Test fun testSuspendCanBeRequestedFromInlineLambdaInLocalVariableInitializerInSuspendContext() =
    singleAndMultiCodegen(
      """
        @Provide suspend fun foo() = Foo()
      """,
      """       
        fun invoke(): suspend () -> Unit = {
          val foo = run { inject<Foo>() }
        }
      """
    )

  @Test fun testComposableCanBeRequestedFromInlineLambdaInLocalVariableInitializerInComposableContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        fun invoke(): @Composable () -> Unit = {
          val foo = run { inject<Foo>() }
        }
      """,
      config = { withCompose() }
    )

  @Test fun testSuspendCanBeRequestedFromInlineLambdaInLocalVariableDelegateInitializerInSuspendContext() =
    singleAndMultiCodegen(
      """
        @Provide suspend fun foo() = Foo()
      """,
      """
        fun invoke(): suspend () -> Unit        = {
          val foo by lazy(run { inject<Foo>() }) {  }
        }
      """
    )

  @Test fun testComposableCanBeRequestedFromInlineLambdaInLocalVariableDelegateInitializerInComposableContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        fun invoke(): @Composable () -> Unit = {
          val foo by lazy(run { inject<Foo>() }) {  }
        }
      """,
      config = { withCompose() }
    )

  @Test fun testSuspendCanBeRequestedFromInlineProviderInSuspendContext() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
      suspend inline fun initialize(@Inject provider: () -> Foo) {
      }
    """,
    """
     fun invoke() = runBlocking { initialize() } 
    """
  )

  @Test fun testComposableCanBeRequestedFromInlineProviderInComposableContext() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
      @Composable inline fun initialize(@Inject provider: () -> Foo) {
      }
    """,
    """
     @Composable fun invoke() { initialize() } 
    """
  )

  @Test fun testCannotRequestSuspendDependencyInDefaultValueOfFunction() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
    """,
    """
      suspend fun invoke(foo: Foo = inject()) {
      }
    """
  ) {
    compilationShouldHaveFailed("injectable com.ivianuu.injekt.integrationtests.foo() of type com.ivianuu.injekt.test.Foo for parameter x of function com.ivianuu.injekt.inject is a suspend function but current call context is default")
  }

  @Test fun testCanRequestComposableDependencyInDefaultValueOfFunction() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
    """,
    """
      @Composable fun invoke(foo: Foo = inject()) {
      }
    """,
    config = { withCompose() }
  )

  @Test fun testCanRequestComposableDependencyInGetterOfComposableProperty() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        val fooGetter: Foo
          @Composable get() = inject()
      """,
      config = { withCompose() }
    )
}
