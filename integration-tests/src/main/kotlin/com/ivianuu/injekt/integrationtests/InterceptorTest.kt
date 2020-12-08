package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
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
            @Interceptor fun <T> intercept(factory: () -> T): () -> T { 
                return {
                    callCount++
                    factory()
                }
            }
            
            @Binding fun foo() = Foo()
            
            @Binding fun bar(foo: Foo) = Bar(foo)
            
            @Binding fun baz(foo: Foo, bar: Bar) = Baz(bar, foo)
            
            @Component interface MyComponent {
                val baz: Baz
            }
            
            fun invoke(): Int {
                create<MyComponent>().baz
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

            @Binding fun foo() = Foo()
            
            @Binding fun bar(foo: Foo) = Bar(foo)
            
            @Binding fun baz(foo: Foo, bar: Bar) = Baz(bar, foo)

            class MyModule {
                @Interceptor fun <T> intercept(factory: () -> T): () -> T { 
                    return {
                        callCount++
                        factory()
                    }
                }
            }
            
            fun invoke(): Int {
                create<Baz>(MyModule())
                return callCount
            }
        """
    ) {
        assertEquals(4, invokeSingleFile<Int>())
    }

    @Test
    fun testExplicitInterceptorInParentInterceptsChild() = codegen(
        """
            var callCount = 0

            @Binding fun foo() = Foo()

            class MyParentModule {
                @Interceptor fun <T : Foo> intercept(factory: () -> T): () -> T { 
                    return {
                        callCount++
                        factory()
                    }
                }
            }
            
            @Component interface ParentComponent {
                val childComponent: () -> MyChildComponent
                
                @Component interface MyChildComponent {
                    val foo: Foo
                }
            }
            
            fun invoke(): Int {
                create<ParentComponent>(MyParentModule()).childComponent().foo
                return callCount
            }
        """
    ) {
        assertEquals(1, invokeSingleFile<Int>())
    }

    @Test
    fun testInterceptorHasState() = codegen(
        """
            @Interceptor fun intercept(factory: () -> Foo): () -> Foo { 
                var instance: Foo? = null
                return {
                    if (instance == null) instance = factory()
                    instance!!
                }
            }
            
            @Binding fun foo() = Foo()
            
            @Component interface MyComponent {
                val foo: Foo
            }
            
            fun invoke(): Pair<Foo, Foo> {
                val component = create<MyComponent>()
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
            @Interceptor fun <S> intercept(factory: S): S = factory

            @Binding fun foo() = Foo()
            
            @Component interface MyComponent {
                val foo: Foo
            }
        """
    )

    @Test
    fun testInterceptorWithDifferentCallContextIsNotApplicable() = codegen(
        """
            var callCount = 0
            @Interceptor fun <T> intercept(factory: suspend () -> T): suspend () -> T { 
                return {
                    callCount++
                    factory()
                }
            }
            
            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val foo: Foo
            }
            
            fun invoke(): Int {
                create<MyComponent>().foo
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
            @Interceptor fun <T> intercept(factory: () -> T): () -> T { 
                return {
                    called = true
                    factory()
                }
            }
            
            @Binding
            suspend fun foo() = Foo()

            @Component interface MyComponent {
                abstract suspend fun foo(): Foo
            }
            
            fun invoke(): Boolean {
                runBlocking { create<MyComponent>().foo() }
                return called
            }
        """
    ) {
        assertFalse(invokeSingleFile<Boolean>())
    }

    @Test
    fun testSuspendInterceptor() = codegen(
        """
            @Interceptor fun intercept(factory: suspend () -> Foo): suspend () -> Foo = factory
            
            @Binding
            suspend fun foo() = Foo()
            
            @Component interface MyComponent {
                abstract suspend fun foo(): Foo
            }
        """
    )

    @Test
    fun testComposableInterceptor() = codegen(
        """
            @Interceptor fun intercept(factory: @Composable () -> Foo): @Composable () -> Foo = factory
            
            @Binding
            @Composable
            fun foo() = Foo()
            
            @Component interface MyComponent {
                @Composable
                val foo: Foo
            }
        """
    )

    @Test
    fun testInterceptorWithTargetComponentOnlyInterceptsBindingsOfTheComponent() = codegen(
        """
            var callCount = 0
            @Bound(TestScope1::class)
            @Interceptor fun <T : Foo> intercept(factory: () -> T): () -> T {
                return {
                    callCount++
                    factory()
                }
            }
            
            @Binding fun foo() = Foo()
            
            @Scoped(TestScope1::class) @Component interface ParentComponent {
                val foo: Foo
                val childFactory: () -> MyChildComponent
                @Scoped(TestScope2::class) @Component interface MyChildComponent {
                    val foo: Foo
                }
            }
            
            fun invoke(): Int {
                val component = create<ParentComponent>()
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
            @Interceptor fun intercept(factory: () -> Foo): () -> Foo = factory
            
            @Scoped(MyComponent::class)
            @Binding fun foo() = Foo()
            
            @Component interface MyComponent {
                val foo: Foo
            }
        """
    )

}
