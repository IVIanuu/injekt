package com.ivianuu.injekt.integrationtests

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
    fun testEagerDecoratedBinding() = codegen(
        """
            var called = false

            @Decorator
            annotation class MyDecorator {
                companion object {
                    fun <T : Foo> decorate(factory: () -> T) = factory
                }
            }

            @Eager
            @MyDecorator
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

}