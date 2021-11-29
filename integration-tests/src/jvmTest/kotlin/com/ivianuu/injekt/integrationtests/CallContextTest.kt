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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.runComposing
import com.ivianuu.injekt.test.singleAndMultiCodegen
import com.ivianuu.injekt.test.withCompose
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
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
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testNonComposableInjectableCanReceiveComposableInjectableInComposableContext() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = runComposing { inject<Bar>() } 
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile().shouldBeTypeOf<Bar>()
  }

  @Test fun testSuspendProviderCanRequestSuspendDependencyInDefaultContext() =
    singleAndMultiCodegen(
      """
        @Provide suspend fun foo() = Foo()
        @Provide fun bar(foo: Foo) = Bar(foo)
      """,
      """
        fun invoke() = inject<suspend () -> Bar>()
      """
    ) {
      runBlocking {
        invokeSingleFile<suspend () -> Bar>()
          .invoke()
          .shouldBeTypeOf<Bar>()
      }
    }

  @Test fun testComposableProviderCanRequestComposableDependencyInDefaultContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
        @Provide fun bar(foo: Foo) = Bar(foo)
      """,
      """
        fun invoke() = inject<@Composable () -> Bar>()
      """,
      config = { withCompose() }
    ) {
      runComposing {
        invokeSingleFile<@Composable () -> Bar>()
          .invoke()
          .shouldBeTypeOf<Bar>()
      }
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
    ) {
      runBlocking {
        invokeSingleFile<suspend () -> Bar>()
          .invoke()
          .shouldBeTypeOf<Bar>()
      }
    }

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
    ) {
      runComposing {
        invokeSingleFile<@Composable () -> Bar>()
          .invoke()
          .shouldBeTypeOf<Bar>()
      }
    }

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
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testComposableCanBeRequestedFromInlineLambdaInComposableContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        fun invoke() = runComposing {
          run {
            run {
              inject<Foo>()
            }
          }
        }
      """,
      config = { withCompose() }
    ) {
      invokeSingleFile()
        .shouldBeTypeOf<Foo>()
    }

  @Test fun testSuspendCanBeRequestedFromLocalVariableInitializerInSuspendContext() =
    singleAndMultiCodegen(
      """
        @Provide suspend fun foo() = Foo()
      """,
      """
        fun invoke() = runBlocking {
          val foo = inject<Foo>()
          foo
        }
      """
    ) {
      invokeSingleFile()
        .shouldBeTypeOf<Foo>()
    }

  @Test fun testComposableCanBeRequestedFromLocalVariableInitializerInComposableContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        fun invoke() = runComposing {
          val foo = inject<Foo>()
          foo
        }
      """,
      config = { withCompose() }
    ) {
      invokeSingleFile()
        .shouldBeTypeOf<Foo>()
    }

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
        fun invoke(): suspend () -> Foo = {
          val foo = run { inject<Foo>() }
          foo
        }
      """
    ) {
      runBlocking {
        invokeSingleFile<suspend () -> Foo>()
          .invoke()
          .shouldBeTypeOf<Foo>()
      }
    }

  @Test fun testComposableCanBeRequestedFromInlineLambdaInLocalVariableInitializerInComposableContext() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        fun invoke(): @Composable () -> Foo = {
          val foo = run { inject<Foo>() }
          foo
        }
      """,
      config = { withCompose() }
    ) {
      runComposing {
        invokeSingleFile<@Composable () -> Foo>()
          .invoke()
          .shouldBeTypeOf<Foo>()
      }
    }

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
      suspend inline fun createFoo(@Inject provider: () -> Foo) = provider()
    """,
    """
     fun invoke() = runBlocking { createFoo() } 
    """
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

  @Test fun testComposableCanBeRequestedFromInlineProviderInComposableContext() = singleAndMultiCodegen(
    """
      @Provide @Composable fun foo() = Foo()
      @Composable inline fun createFoo(@Inject provider: () -> Foo) = provider()
    """,
    """
     fun invoke() = runComposing { createFoo() } 
    """,
    config = { withCompose() }
  ) {
    invokeSingleFile()
      .shouldBeTypeOf<Foo>()
  }

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
      @Composable fun foo(foo: Foo = inject()) = foo

      fun invoke(): @Composable () -> Foo = { foo() }
    """,
    config = { withCompose() }
  ) {
    runComposing {
      invokeSingleFile<@Composable () -> Foo>()
        .invoke()
        .shouldBeTypeOf<Foo>()
    }
  }

  @Test fun testCanRequestComposableDependencyInGetterOfComposableProperty() =
    singleAndMultiCodegen(
      """
        @Provide @Composable fun foo() = Foo()
      """,
      """
        val fooGetter: Foo
          @Composable get() = inject()

        fun invoke(): @Composable () -> Foo = { fooGetter }
      """,
      config = { withCompose() }
    ) {
      runComposing {
        invokeSingleFile<@Composable () -> Foo>()
          .invoke()
          .shouldBeTypeOf<Foo>()
      }
    }
}
