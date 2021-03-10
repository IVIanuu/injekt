/*
 * Copyright 2020 Manuel Wrage
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
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class CallContextTest {

    @Test
    fun testSuspendCannotBeRequestedFromNonSuspend() = codegen(
        """
            @Given suspend fun foo() = Foo()
            @Given suspend fun bar(@Given foo: Foo) = Bar(foo)
            @Composable fun invoke() = given<Bar>()
        """
    ) {
        compilationShouldHaveFailed("current call context is COMPOSABLE but com.ivianuu.injekt.integrationtests.bar is SUSPEND")
    }

    @Test
    fun testNonSuspendGivenCanReceiveSuspendGivenInSuspendContext() = codegen(
        """
            @Given suspend fun foo() = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            fun invoke() = runBlocking { given<Bar>() }
        """
    )

    @Test
    fun testComposableDependencyCannotBeRequestedFromNonComposable() = codegen(
        """
            @Given @Composable fun foo() = Foo()
            @Given @Composable fun bar(@Given foo: Foo) = Bar(foo)
            suspend fun invoke() {
                given<Bar>()
            }
        """
    ) {
        compilationShouldHaveFailed("current call context is SUSPEND but com.ivianuu.injekt.integrationtests.bar is COMPOSABLE")
    }

    @Test
    fun testCanRequestSuspendDependencyFromNonSuspendFunctionInSuspendLambda() = codegen(
        """
            @Given suspend fun foo() = Foo()
            @Given fun lazyBar(): suspend () -> Bar = { Bar(given()) }
            fun invoke() {
                given<suspend () -> Bar>()
            }
        """
    )

    @Test
    fun testSuspendProviderCanRequestSuspendDependencyInDefaultContext() = codegen(
        """
            @Given suspend fun foo() = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            fun invoke() = given<suspend () -> Bar>()
        """
    )

    @Test
    fun testCanRequestSuspendDependencyFromNonSuspendFunctionInSuspendLambdaWithAlias() = codegen(
        """
            typealias SuspendFactory<T> = suspend () -> T
            @Given suspend fun foo() = Foo()
            @Given fun lazyBar(): SuspendFactory<Bar> = { Bar(given()) }
            fun invoke() {
                given<SuspendFactory<Bar>>()
            }
        """
    )

    @Test
    fun testCanRequestComposableDependencyFromNonComposableFunctionInComposableLambda() = codegen(
        """
            @Given @Composable fun foo() = Foo()
            @Given fun lazyBar(): @Composable () -> Bar = { Bar(given()) }
            fun invoke() {
                given<@Composable () -> Bar>()
            }
        """
    )

    @Test
    fun testCanRequestComposableDependencyFromNonComposableFunctionInComposableLambdaWithAlias() =
        codegen(
            """
            typealias ComposableFactory<T> = @Composable () -> T
            @Given @Composable fun foo() = Foo()
            @Given fun lazyBar(): ComposableFactory<Bar> = { Bar(given()) }
            fun invoke() {
                given<ComposableFactory<Bar>>()
            }
        """
        )

}
