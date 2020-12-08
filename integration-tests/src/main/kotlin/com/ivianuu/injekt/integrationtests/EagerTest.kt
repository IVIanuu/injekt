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
            @Binding fun foo(): Foo {
                called = true
                return Foo()
            }

            @Component interface MyComponent {
                val foo: Foo
            }

            fun invoke(): Boolean {
                val component = create<MyComponent>()
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

            @Interceptor fun interceptFoo(factory: () -> Foo) = factory

            @Eager @Binding fun foo(): Foo {
                called = true
                return Foo()
            }

            @Component interface MyComponent {
                val foo: Foo
            }

            fun invoke(): Boolean {
                val component = create<MyComponent>()
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
            @Scoped(TestScope1::class)
            @Binding fun foo(): Foo {
                called = true
                return Foo()
            }

            @Scoped(TestScope1::class) @Component interface MyComponent {
                val foo: Foo
            }

            fun invoke(): Boolean {
                val component = create<MyComponent>()
                return called
            }
        """
    ) {
        assertTrue(invokeSingleFile<Boolean>())
    }

    @Test
    fun testEagerScopedBindingWithScopedDependencies() = codegen(
        """
            @Scoped(MyComponent::class)
            @Binding fun foo() = Foo()

            @Eager
            @Scoped(MyComponent::class)
            @Binding fun bar(foo: Foo) = Bar(foo)

            @Component interface MyComponent {
                val bar: Bar
            }
        """
    )

    @Test
    fun testEagerScopedCircularDependency() = codegen(
        """
            @Eager
            @Scoped(MyComponent::class)
            @Binding fun foo(bar: () -> Bar) = Foo()

            @Eager
            @Scoped(MyComponent::class)
            @Binding fun bar(foo: () -> Foo) = Bar(Foo())

            @Component interface MyComponent {
                val bar: Bar
            }
        """
    )

    @Test
    fun testEagerAssistedBindingFails() = codegen(
        """
            @Eager @Binding fun bar(foo: Foo) = Bar(foo)
            fun invoke() = create<(Foo) -> Bar>()
        """
    ) {
        assertInternalError("Cannot perform assisted injection on a eager binding")
    }

}