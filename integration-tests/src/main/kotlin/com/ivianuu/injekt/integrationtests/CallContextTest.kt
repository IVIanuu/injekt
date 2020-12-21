package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
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
        assertCompileError("current call context is COMPOSABLE but com.ivianuu.injekt.integrationtests.bar is SUSPEND")
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
        assertCompileError("current call context is SUSPEND but com.ivianuu.injekt.integrationtests.bar is COMPOSABLE")
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
