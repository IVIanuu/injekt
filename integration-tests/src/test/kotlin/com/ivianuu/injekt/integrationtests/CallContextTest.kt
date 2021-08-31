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
import org.junit.Test

class CallContextTest {
  @Test fun testSuspendCannotBeRequestedFromNonSuspend() = singleAndMultiCodegen(
    """
      @Provide suspend fun foo() = Foo()
      @Provide suspend fun bar(foo: Foo) = Bar(foo)
    """,
    """
      @Composable fun invoke() = inject<Bar>()
    """
  ) {
    compilationShouldHaveFailed("injectable com.ivianuu.injekt.integrationtests.bar() of type com.ivianuu.injekt.test.Bar for parameter value of function com.ivianuu.injekt.inject is a suspend function but current call context is composable")
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

  @Test fun testComposableDependencyCannotBeRequestedFromNonComposable() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
      @Provide @Composable fun bar(foo: Foo) = Bar(foo)
    """,
    """
      suspend fun invoke() = inject<Bar>()
    """
  ) {
    compilationShouldHaveFailed("injectable com.ivianuu.injekt.integrationtests.bar() of type com.ivianuu.injekt.test.Bar for parameter value of function com.ivianuu.injekt.inject is a composable function but current call context is suspend")
  }

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

  @Test fun testCanRequestSuspendDependencyFromNonSuspendFunctionInSuspendLambdaWithAlias() =
    singleAndMultiCodegen(
      """
        typealias SuspendFactory<T> = suspend () -> T
        @Provide suspend fun foo() = Foo()
        @Provide fun lazyBar(): SuspendFactory<Bar> = { Bar(inject()) }
      """,
      """
        fun invoke() = inject<SuspendFactory<Bar>>()
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
      """
    )

  @Test
  fun testCanRequestComposableDependencyFromNonComposableFunctionInComposableLambdaWithAlias() =
    singleAndMultiCodegen(
      """
        typealias ComposableFactory<T> = @Composable () -> T
        @Provide @Composable fun foo() = Foo()
        @Provide fun lazyBar(): ComposableFactory<Bar> = { Bar(inject()) }
      """,
      """
        fun invoke() = inject<ComposableFactory<Bar>>()
      """
    )

  @Test fun testSuspendCanBeRequestedFromInlineLambdaInSuspendContext() = singleAndMultiCodegen(
    """
      @Provide suspend fun suspendFoo() = Foo()
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
        @Provide @Composable fun composableFoo() = Foo()
      """,
      """
        @Composable fun invoke() = run {
          run {
            inject<Foo>()
          }
        }
      """
    )

  @Test fun testSuspendCanBeRequestFromInlineProviderInSuspendContext() = singleAndMultiCodegen(
    """
      @Provide suspend fun suspendFoo() = Foo()
      suspend inline fun initialize(@Inject provider: () -> Foo) {
      }
    """,
    """
     fun invoke() = runBlocking { initialize() } 
    """
  )

  @Test fun testCanRequestComposableDependencyInGetterOfComposableProperty() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun composableFoo() = Foo()
      """,
      """
        val fooGetter: Foo
          @Composable get() = inject()
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
    compilationShouldHaveFailed("injectable com.ivianuu.injekt.integrationtests.foo() of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.inject is a suspend function but current call context is default")
  }

  @Test fun testCanRequestComposableDependencyInDefaultValueOfFunction() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
    """,
    """
      @Composable fun invoke(foo: Foo = inject()) {
      }
    """
  )
}
