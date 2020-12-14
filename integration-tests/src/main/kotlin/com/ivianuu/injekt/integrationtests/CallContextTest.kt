package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class CallContextTest {
    @Test
    fun testSuspendCannotBeRequestedFromComposable() = codegen(
        """
            @Given suspend fun foo() = Foo()
            @Given suspend fun bar(foo: Foo = given) = Bar(foo)
            @Composable fun invoke() {
                given<Bar>()
            }
        """
    ) {
        assertCompileError("current call context is COMPOSABLE but com.ivianuu.injekt.integrationtests.bar is SUSPEND")
    }

    @Test
    fun testComposableDependencyCannotBeRequestedFromSuspend() = codegen(
        """
            @Given @Composable fun foo() = Foo()
            @Given @Composable fun bar(foo: Foo = given) = Bar(foo)
            suspend fun invoke() {
                given<Bar>()
            }
        """
    ) {
        assertCompileError("current call context is SUSPEND but com.ivianuu.injekt.integrationtests.bar is COMPOSABLE")
    }
}
