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

            @Component abstract class MyComponent {
                abstract val bar: Bar
            }
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

            @Component abstract class MyComponent {
                abstract val bar: Bar
            }
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

            @Component abstract class MyComponent {
                abstract val baz: Baz
            }
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

            @Component abstract class MyComponent {
                @Composable
                abstract val bar: Bar
            }
        """
    )

    @Test
    fun testComponentCannotRequestDependencyWithDifferentCallContext() = codegen(
        """
            @Binding
            @Composable
            fun foo() = Foo()

            @Component abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    ) {
        assertInternalError("Call context mismatch")
    }

}