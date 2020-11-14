package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertSame
import org.junit.Test

class DecoratorTest {

    @Test
    fun testExplicitDecorator() = codegen(
        """
            var callCount = 0
            @Decorator
            annotation class MyDecorator {
                companion object {
                    fun <T> decorate(myComponent: MyComponent, factory: () -> T): () -> T {
                        return {
                            callCount++
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
            
            fun invoke(): Int {
                component<MyComponent>().foo
                return callCount
            }
        """
    ) {
        assertEquals(1, invokeSingleFile<Int>())
    }

    @Test
    fun testExplicitDecoratorWithMultipleCallables() = codegen(
        """
            val calls = mutableListOf<String>()
            @Decorator
            annotation class MyDecorator {
                companion object {
                    fun <T> a(myComponent: MyComponent, factory: () -> T): () -> T {
                        return {
                            calls += "a"
                            factory()
                        }
                    }
                    fun <T> b(myComponent: MyComponent, factory: () -> T): () -> T {
                        return {
                            calls += "b"
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
            
            fun invoke(): List<String> {
                component<MyComponent>().foo
                return calls
            }
        """
    ) {
        val calls = invokeSingleFile<List<String>>()
        assertEquals(listOf("b", "a"), calls)
    }

    @Test
    fun testExplicitDecoratorWithAnnotationValueParam() = codegen(
        """
            var arg = "off"
            @Decorator
            annotation class MyDecorator(val value: String) {
                companion object {
                    fun <T> a(@Arg("value") _arg: String, factory: () -> T): () -> T {
                        return {
                            arg = _arg
                            factory()
                        }
                    }
                }
            }
            
            @MyDecorator("on")
            @Binding
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
            
            fun invoke(): String {
                component<MyComponent>().foo
                return arg
            }
        """
    ) {
        assertEquals("on", invokeSingleFile<String>())
    }

    @Test
    fun testExplicitDecoratorWithAnnotationTypeParam() = codegen(
        """
            @Decorator
            annotation class MyDecorator<T> {
                companion object {
                    fun <@Arg("T") T, S> a(arg: T, factory: () -> S): () -> S {
                        return {
                            factory()
                        }
                    }
                }
            }
            
            @MyDecorator<String>
            @Binding
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    ) {
        assertInternalError("No binding found for 'kotlin.String'")
    }

    @Test
    fun testGlobalImplicitDecorator() = codegen(
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
    fun testLocalImplicitDecorator() = codegen(
        """
            var callCount = 0

            @Binding
            fun foo() = Foo()
            
            @Binding
            fun bar(foo: Foo) = Bar(foo)
            
            @Binding
            fun baz(foo: Foo, bar: Bar) = Baz(bar, foo)
            
            @Component
            abstract class MyComponent {
                abstract val baz: Baz
                
                @Decorator
                fun <T> decorate(factory: () -> T): () -> T { 
                    return {
                        callCount++
                        factory()
                    }
                }
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
    fun testImplicitDecoratorInParentDecoratesChild() = codegen(
        """
            var callCount = 0

            @Binding
            fun foo() = Foo()
            
            @Component
            abstract class ParentComponent {
                abstract val childComponent: () -> MyChildComponent
            
                @Decorator
                fun <T : Foo> decorate(factory: () -> T): () -> T { 
                    return {
                        callCount++
                        factory()
                    }
                }
                
                @ChildComponent
                abstract class MyChildComponent {
                    abstract val foo: Foo
                }
            }
            
            fun invoke(): Int {
                component<ParentComponent>().childComponent().foo
                return callCount
            }
        """
    ) {
        assertEquals(1, invokeSingleFile<Int>())
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
    fun testDecoratorWithGenericReturnType() = codegen(
        """
            @Decorator
            annotation class MyDecorator {
                companion object {
                    fun <S> decorate(factory: S): S = factory
                }
            }

            @MyDecorator
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    )

    @Test
    fun testDecoratorWithDifferentCallContextIsNotApplicable() = codegen(
        """
            var callCount = 0
            @Decorator
            fun <T> decorate(factory: suspend () -> T): suspend () -> T { 
                return {
                    callCount++
                    factory()
                }
            }
            
            @Binding
            fun foo() = Foo()

            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
            
            fun invoke(): Int {
                component<MyComponent>().foo
                return callCount
            }
        """
    ) {
        assertEquals(0, invokeSingleFile<Int>())
    }

    @Test
    fun testDecoratorWithDifferentCallContextIsNotApplicable2() = codegen(
        """
            var called = false
            @Decorator
            fun <T> decorate(factory: () -> T): () -> T { 
                return {
                    called = true
                    factory()
                }
            }
            
            @Binding
            suspend fun foo() = Foo()

            @Component
            abstract class MyComponent {
                abstract suspend fun foo(): Foo
            }
            
            fun invoke(): Boolean {
                runBlocking { component<MyComponent>().foo() }
                return called
            }
        """
    ) {
        assertFalse(invokeSingleFile<Boolean>())
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
