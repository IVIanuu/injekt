package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertTrue
import org.junit.Test

class EagerTest {

    @Test
    fun testEagerCreatesOnStart() = codegen(
        """
            var called = false

            @Eager
            @Binding
            fun foo(): Foo {
                called = true
                return Foo()
            }

            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }

            fun invoke(): Boolean {
                val component = component<MyComponent>()
                return called
            }
        """
    ) {
        assertTrue(invokeSingleFile<Boolean>())
    }

    @Test
    fun testEagerInterceptedBinding() = codegen(
        """
            var called = false

            @Interceptor
            fun interceptFoo(factory: () -> Foo) = factory

            @Eager
            @Binding
            fun foo(): Foo {
                called = true
                return Foo()
            }

            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }

            fun invoke(): Boolean {
                val component = component<MyComponent>()
                return called
            }
        """
    ) {
        assertTrue(invokeSingleFile<Boolean>())
    }

    @Test
    fun testEagerScopedBinding() = codegen(
        """
            var called = false

            @Eager
            @Binding(MyComponent::class)
            fun foo(): Foo {
                called = true
                return Foo()
            }

            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }

            fun invoke(): Boolean {
                val component = component<MyComponent>()
                return called
            }
        """
    ) {
        assertTrue(invokeSingleFile<Boolean>())
    }

    @Test
    fun testEagerScopedBindingWithScopedDependencies() = codegen(
        """
            @Binding(MyComponent::class)
            fun foo() = Foo()

            @Eager
            @Binding(MyComponent::class)
            fun bar(foo: Foo) = Bar(foo)

            @Component
            abstract class MyComponent {
                abstract val bar: Bar
            }
        """
    )

    @Test
    fun testEagerScopedCircularDependency() = codegen(
        """
            @Eager
            @Binding(MyComponent::class)
            fun foo(bar: () -> Bar) = Foo()

            @Eager
            @Binding(MyComponent::class)
            fun bar(foo: () -> Foo) = Bar(Foo())

            @Component
            abstract class MyComponent {
                abstract val bar: Bar
            }
        """
    )

    @Test
    fun testEagerAssistedBindingFails() = codegen(
        """
            @Eager
            @Binding
            fun bar(foo: Foo) = Bar(foo)

            @Component
            abstract class MyComponent {
                abstract val barFactory: (Foo) -> Bar
            }
        """
    ) {
        assertInternalError("Cannot perform assisted injection on a eager binding")
    }

}