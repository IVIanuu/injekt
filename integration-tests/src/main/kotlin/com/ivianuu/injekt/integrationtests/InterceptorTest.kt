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
    fun testExplicitInterceptor() = codegen(
        """
            var callCount = 0
            @Interceptor
            annotation class MyInterceptor {
                companion object {
                    fun <T> intercept(myComponent: MyComponent, factory: () -> T): () -> T {
                        return {
                            callCount++
                            factory()
                        }
                    }
                }
            }
            
            @MyInterceptor
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
    fun testExplicitInterceptorWithMultipleCallables() = codegen(
        """
            val calls = mutableListOf<String>()
            @Interceptor
            annotation class MyInterceptor {
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
            
            @MyInterceptor
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
    fun testExplicitInterceptorWithAnnotationValueParam() = codegen(
        """
            var arg = "off"
            @Interceptor
            annotation class MyInterceptor(val value: String) {
                companion object {
                    fun <T> a(@Arg("value") _arg: String, factory: () -> T): () -> T {
                        return {
                            arg = _arg
                            factory()
                        }
                    }
                }
            }
            
            @MyInterceptor("on")
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
    fun testExplicitInterceptorWithAnnotationTypeParam() = codegen(
        """
            @Interceptor
            annotation class MyInterceptor<T> {
                companion object {
                    fun <@Arg("T") T, S> a(arg: T, factory: () -> S): () -> S {
                        return {
                            factory()
                        }
                    }
                }
            }
            
            @MyInterceptor<String>
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
    fun testGlobalImplicitInterceptor() = codegen(
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
    fun testLocalImplicitInterceptor() = codegen(
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
            annotation class Scoped {
                companion object {
                    fun <T> intercept(factory: () -> T): () -> T { 
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
    fun testInterceptorWithGenericReturnType() = codegen(
        """
            @Interceptor
            annotation class MyInterceptor {
                companion object {
                    fun <S> intercept(factory: S): S = factory
                }
            }

            @MyInterceptor
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
            annotation class MyInterceptor {
                companion object {
                    fun <T> intercept(factory: suspend () -> T): suspend () -> T = factory
                }
            }
            
            @MyInterceptor
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
            annotation class MyInterceptor {
                companion object {
                    fun <T> intercept(factory: @Composable () -> T): @Composable () -> T = factory
                }
            }
            
            @MyInterceptor
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
    fun testInterceptorWithDifferentTargetComponentFails() = codegen(
        """
            @Bound(Any::class)
            @Interceptor
            annotation class MyInterceptor {
                companion object {
                    fun <T> intercept(factory: @Composable () -> T): @Composable () -> T = factory
                }
            }
            
            @MyInterceptor
            @Scoped(MyComponent::class)
            @Binding
            @Composable
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                @Composable
                abstract val foo: Foo
            }
        """
    ) {
        assertInternalError("Target component mismatch")
    }

    @Test
    fun testInterceptorsWithDifferentTargetComponentFails() = codegen(
        """
            @Bound(Any::class)
            @Interceptor
            annotation class MyInterceptor1 {
                companion object {
                    fun <T> intercept(factory: @Composable () -> T): @Composable () -> T = factory
                }
            }
            
            @Bound(String::class)
            @Interceptor
            annotation class MyInterceptor2 {
                companion object {
                    fun <T> intercept(factory: @Composable () -> T): @Composable () -> T = factory
                }
            }
            
            @MyInterceptor1
            @MyInterceptor2
            @Composable
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                @Composable
                abstract val foo: Foo
            }
        """
    ) {
        assertInternalError("Target component mismatch")
    }

    @Test
    fun testInterceptorWithUpperBoundsWithTypeAlias() = codegen(
        """
            interface Scope
                   
            interface Flow<T>
            interface MutableFlow<T> : Flow<T>
            
            typealias EffectBlock<S> = suspend (S) -> Unit
            
            @Effect
            annotation class StateEffect { 
                companion object {
                    @SetElements
                    fun <T : suspend (S) -> Unit, S> bind(
                        instance: @ForEffect T
                    ): Set<EffectBlock<S>> = error("")
                }
            }
            
            @Interceptor
            fun <T : Flow<S>, S> intercept(
                effects: Set<EffectBlock<S>>?,
                factory: () -> T
            ): () -> T = factory
            
            @Effect
            annotation class UiStoreBinding {
                companion object {
                    @Binding
                    inline fun <reified T : MutableFlow<S>, reified S> uiStore(
                        noinline provider: (Scope) -> @ForEffect T
                    ): MutableFlow<S> = error("")
                }
            }
            
            @Qualifier
            @Target(AnnotationTarget.TYPE)
            annotation class UiState
            @Binding
            fun <S> MutableFlow<S>.latest(): @UiState S = error("")
            
            interface AState
    
            @UiStoreBinding
            fun Scope.AStore(): MutableFlow<AState> = error("")
            
            @StateEffect
            @FunBinding
            suspend fun AEffect(@FunApi state: AState) {
            }
            
            interface BState
    
            @UiStoreBinding
            fun Scope.BStore(): MutableFlow<BState> = error("") 
            
            @Component
            abstract class MyComponent {
                abstract val aState: @UiState AState
                abstract val bState: @UiState BState
            }  
        """
    )

    @Test
    fun testInterceptorWithoutFactoryAsLastParameter() = codegen(
        """
            @Interceptor
            annotation class MyInterceptor { 
                companion object {
                    fun <T, S> intercept(): () -> T = factory
                }
            }
            
            @MyInterceptor
            class AnnotatedBar(val foo: Foo)
        """
    ) {
        assertInternalError("Interceptor")
    }

    @Test
    fun testInterceptorWithWrongReturnType() = codegen(
        """
            @Interceptor
            annotation class MyInterceptor { 
                companion object {
                    fun <T, S> intercept(factory: () -> T): () -> T = factory
                }
            }
            
            @MyInterceptor
            class AnnotatedBar(val foo: Foo)
        """
    ) {
        assertInternalError("Couldn't resolve all type arguments")
    }

    @Test
    fun testInterceptorWithCorruptTypeParameters() = codegen(
        """
            @Interceptor
            annotation class MyInterceptor { 
                companion object {
                    fun <T, S> intercept(factory: () -> T): () -> T = factory
                }
            }
            
            @MyInterceptor
            class AnnotatedBar(val foo: Foo)
        """
    ) {
        assertInternalError("Couldn't resolve all type arguments")
    }

    @Test
    fun testInterceptorTargetNotInBoundsFails() = codegen(
        """
            @Interceptor
            annotation class MyInterceptor { 
                companion object {
                    fun <T : String> intercept(factory: () -> T): () -> T = factory
                }
            }
            
            @MyInterceptor
            class AnnotatedBar(val foo: Foo)
        """
    ) {
        assertInternalError("is not a sub type of")
    }

    @Test
    fun testScopedBindingWithInterceptor() = codegen(
        """
            @Interceptor
            annotation class MyInterceptor {
                companion object {
                    fun <T> intercept(factory: () -> T): () -> T = factory
                }
            }
            
            @MyInterceptor
            @Binding(MyComponent::class)
            fun foo() = Foo()
            
            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    )

}
