package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class LambdaContributionsTest {

    @Test
    fun testBindingLambda() = codegen(
        """
            val barProvider: @Binding (Foo) -> Bar = { Bar(it) }
            @Module val barModule = moduleOf(barProvider)

            @Binding
            fun foo() = Foo()

            @Component
            abstract class MyComponent {
                abstract val bar: Bar
            }
        """
    )

    @Test
    fun testMapEntriesLambda() = codegen(
        """
            inline fun <reified T : Any> classMapProviderFactory(): @MapEntries (T) -> Map<KClass<*>, Any> = { 
                mapOf(T::class to it)
            }
            @Module val fooMapModule = classMapProviderFactory<Foo>()

            @Binding
            fun foo() = Foo()

            @Component
            abstract class MyComponent {
                abstract val classMap: Map<KClass<*>, Any>
            }
        """
    )

    @Test
    fun testSetElementsLambda() = codegen(
        """
            fun <T> setProviderFactory(): @SetElements (T) -> Set<Any> = { 
                setOf(it)
            }
            @Module val fooSetModule = setProviderFactory<Foo>()

            @Binding
            fun foo() = Foo()

            @Component
            abstract class MyComponent {
                abstract val set: Set<Any>
            }
        """
    )

    @Test
    fun testInterceptorLambda() = codegen(
        """
            fun <T> interceptorFactory(): @Interceptor (() -> T) -> () -> T = { it }
           
            @Module val fooInterceptorModule = interceptorFactory<Foo>()

            @Binding
            fun foo() = Foo()

            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    )

    @Test
    fun testModuleLambda() = codegen(
        """
            class Dep<T>(val value: T)
            fun <T> depModuleFactory(): @Module () -> @Binding (T) -> Dep<T> = { { Dep(it) } }
            @Module val fooSetModule: @Module () -> @Module () -> @Binding (Foo) -> Dep<Foo> = { depModuleFactory<Foo>() }

            @Binding
            fun foo() = Foo()

            @Component
            abstract class MyComponent {
                abstract val dep: Dep<Foo>
            }
        """
    )

}
