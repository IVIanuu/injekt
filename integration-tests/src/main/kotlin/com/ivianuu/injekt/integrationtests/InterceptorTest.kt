package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertSame
import org.junit.Test

class InterceptorTest {

    @Test
    fun testImplicitInterceptor() = codegen(
        """
            var callCount = 0
            @Interceptor
            fun <T> intercept(factory: () -> T): () -> T { 
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
    fun testExplicitInterceptor() = codegen(
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
                
                @Interceptor
                fun <T> intercept(factory: () -> T): () -> T { 
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
    fun testImplicitInterceptorInParentInterceptsChild() = codegen(
        """
            var callCount = 0

            @Binding
            fun foo() = Foo()
            
            @Component
            abstract class ParentComponent {
                abstract val childComponent: () -> MyChildComponent
            
                @Interceptor
                fun <T : Foo> intercept(factory: () -> T): () -> T { 
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
    fun testInterceptorHasState() = codegen(
        """
            @Interceptor
            fun intercept(factory: () -> Foo): () -> Foo { 
                var instance: Foo? = null
                return {
                    if (instance == null) instance = factory()
                    instance!!
                }
            }
            
            @Binding
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
    fun testInterceptorWithGenericReturnType() = codegen(
        """
            @Interceptor
            fun <S> intercept(factory: S): S = factory

            @Binding
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    )

    @Test
    fun testInterceptorWithDifferentCallContextIsNotApplicable() = codegen(
        """
            var callCount = 0
            @Interceptor
            fun <T> intercept(factory: suspend () -> T): suspend () -> T { 
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
    fun testInterceptorWithDifferentCallContextIsNotApplicable2() = codegen(
        """
            var called = false
            @Interceptor
            fun <T> intercept(factory: () -> T): () -> T { 
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
    fun testSuspendInterceptor() = codegen(
        """
            @Interceptor
            fun intercept(factory: suspend () -> Foo): suspend () -> Foo = factory
            
            @Binding
            suspend fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract suspend fun foo(): Foo
            }
        """
    )

    @Test
    fun testComposableInterceptor() = codegen(
        """
            @Interceptor
            fun intercept(factory: @Composable () -> Foo): @Composable () -> Foo = factory
            
            @Binding
            @Composable
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                @Composable
                abstract val foo: Foo
            }
        """
    )

    @Test
    fun testInterceptorWithTargetComponentOnlyInterceptsBindingsOfTheComponent() = codegen(
        """
            var callCount = 0
            @Bound(ParentComponent::class)
            @Interceptor
            fun <T : Foo> intercept(factory: () -> T): () -> T {
                return {
                    callCount++
                    factory()
                }
            }
            
            @Binding
            fun foo() = Foo()
            
            @Component
            abstract class ParentComponent {
                abstract val foo: Foo
                abstract val childFactory: () -> MyChildComponent
                @ChildComponent
                abstract class MyChildComponent {
                    abstract val foo: Foo
                }
            }
            
            fun invoke(): Int {
                val component = component<ParentComponent>()
                component.foo
                component.childFactory().foo
                return callCount
            }
        """
    ) {
        assertEquals(1, invokeSingleFile<Int>())
    }

    @Test
    fun testScopedBindingWithInterceptor() = codegen(
        """
            @Interceptor
            fun intercept(factory: () -> Foo): () -> Foo = factory
            
            @Scoped(MyComponent::class)
            @Binding
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    )

}
