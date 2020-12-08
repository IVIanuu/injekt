package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class CallContextTest {

    @Test
    fun testSuspendDependencyCannotBeRequestedFromComposable() = codegen(
        """
            @Binding
            @Composable
            fun foo() = Foo()

            @Binding
            suspend fun bar(foo: Foo) = Bar(foo)

            fun invoke() = create<Bar>()
        """
    ) {
        assertInternalError("Call context mismatch")
    }

    @Test
    fun testComposableDependencyCannotBeRequestedFromSuspend() = codegen(
        """
            @Binding
            suspend fun foo() = Foo()

            @Binding
            @Composable
            fun bar(foo: Foo) = Bar(foo)

            fun invoke() = create<Bar>()
        """
    ) {
        assertInternalError("Call context mismatch")
    }

    @Test
    fun testBindingCannotMixedCallContextDependencies() = codegen(
        """
            @Binding
            suspend fun foo() = Foo()

            @Binding
            @Composable
            fun bar() = Bar(Foo())
            
            @Binding fun baz(bar: Bar, foo: Foo) = Baz(bar, foo)

            fun invoke() = create<Baz>()
        """
    ) {
        assertInternalError("Dependencies call context mismatch")
    }

    @Test
    fun testBindingAdaptsCallContextOfDependency() = codegen(
        """
            @Binding
            @Composable
            fun foo() = Foo()

            @Binding fun bar(foo: Foo) = Bar(foo)

            @Component interface MyComponent {
                @Composable
                val bar: Bar
            }
        """
    )

    @Test
    fun testComponentCannotRequestDependencyWithDifferentCallContext() = codegen(
        """
            @Binding
            @Composable
            fun foo() = Foo()
            fun invoke() = create<Foo>()
        """
    ) {
        assertInternalError("Call context mismatch")
    }

}