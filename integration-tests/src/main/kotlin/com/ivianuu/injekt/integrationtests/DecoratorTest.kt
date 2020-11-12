package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class DecoratorTest {

    @Test
    fun testExplicitDecorator() = codegen(
        """
            var called = false
            @Decorator
            annotation class MyDecorator {
                companion object {
                    fun <T> decorate(myComponent: MyComponent, factory: () -> T): () -> T {
                        return {
                            called = true
                            factory()
                        }
                    }
                }
            }
            
            @MyDecorator
            @Binding
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
            
            fun invoke(): Boolean {
                component<MyComponent>().foo
                return called
            }
        """
    ) {
        assertTrue(invokeSingleFile<Boolean>())
    }

    @Test
    fun testImplicitDecorator() = codegen(
        """
            var callCount = 0
            @Decorator
            fun <T> decorate(factory: () -> T): () -> T { 
                return {
                    callCount++
                    factory()
                }
            }
            
            @Binding
            fun foo() = Foo()
            
            @Binding
            fun bar(foo: Foo) = Bar(foo)
            
            @Binding
            fun baz(foo: Foo, bar: Bar) = Baz(bar, foo)
            
            @Component
            abstract class MyComponent {
                abstract val baz: Baz
            }
            
            fun invoke(): Int {
                component<MyComponent>().baz
                return callCount
            }
        """
    ) {
        assertEquals(4, invokeSingleFile<Int>())
    }

    @Test
    fun testDecoratorHasState() = codegen(
        """
            @Decorator
            annotation class Scoped {
                companion object {
                    fun <T> decorate(factory: () -> T): () -> T { 
                        var instance: T? = null
                        return {
                            if (instance == null) instance = factory()
                            instance as T
                        }
                    }
                }
            }
            
            @Scoped
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
            
            fun invoke(): Pair<Foo, Foo> {
                val component = component<MyComponent>()
                return component.foo to component.foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testSuspendDecorator() = codegen(
        """
            @Decorator
            annotation class MyDecorator {
                companion object {
                    fun <T> decorate(factory: suspend () -> T): suspend () -> T = factory
                }
            }
            
            @MyDecorator
            suspend fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract suspend fun foo(): Foo
            }
        """
    )

    @Test
    fun testComposableDecorator() = codegen(
        """
            @Decorator
            annotation class MyDecorator {
                companion object {
                    fun <T> decorate(factory: @Composable () -> T): @Composable () -> T = factory
                }
            }
            
            @MyDecorator
            @Composable
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                @Composable
                abstract val foo: Foo
            }
        """
    )

}