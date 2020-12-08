package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.setGenerateMergeComponents
import com.ivianuu.injekt.test.source
import org.junit.Assert.assertTrue
import org.junit.Test

class LambdaContributionsTest {

    @Test
    fun testBindingLambda() = codegen(
        """
            val barProvider: @Binding (Foo) -> Bar = { Bar(it) }
            @Module val barModule = moduleOf(moduleOf(barProvider))

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val bar: Bar
            }
        """
    )

    @Test
    fun testBindingLambdaExpression() = codegen(
        """
            @Module val barModule = @Scoped @Binding { foo: Foo -> Bar(foo) }

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val bar: Bar
            }
        """
    )

    @Test
    fun testBindingLambdaExpressionMulti() = multiCodegen(
        listOf(
            source(
                """
                        @MergeComponent interface MyComponent {
                            val bar: Bar
                        }
                        """
            )
        ),
        listOf(
            source(
                """
                        @Module val barModule = @Scoped(MyComponent::class) @Binding { foo: Foo -> Bar(foo) }
        """
            )
        ),
        listOf(
            source(
                """
                        @Binding fun foo() = Foo()
                """
            )
        ),
        config = { if (it == 2) setGenerateMergeComponents(true) }
    )

    @Test
    fun testTypeAliasBindingLambda() = codegen(
        """
            typealias BarFactory = @Binding (Foo) -> Bar
            val barProvider: BarFactory = { Bar(it) }
            @Module val barModule = moduleOf(barProvider)

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val bar: Bar
            }
        """
    )

    @Test
    fun testTypeAliasBindingLambda2() = codegen(
        """
            typealias BarFactory = (Foo) -> Bar
            val barProvider: @Binding BarFactory = { Bar(it) }
            @Module val barModule = moduleOf(barProvider)

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val bar: Bar
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

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val classMap: Map<KClass<*>, Any>
            }
        """
    )

    @Test
    fun testSetElementsLambda() = codegen(
        """
            fun <T : Any> setProviderFactory(): @SetElements (T) -> Set<Any> = { 
                setOf(it)
            }
            @Module val fooSetModule = setProviderFactory<Foo>()

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val set: Set<Any>
            }
        """
    )

    @Test
    fun testInterceptorLambda() = codegen(
        """
            var called = false
            fun <T> interceptorFactory(): @Interceptor (() -> T) -> () -> T = { called = true; it }
           
            @Module val fooInterceptorModule = interceptorFactory<Foo>()

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val foo: Foo
            }

            fun invoke(): Boolean {
                create<MyComponent>().foo
                return called
            }
        """
    ) {
        assertTrue(invokeSingleFile<Boolean>())
    }

    @Test
    fun testMultipleInterceptorLambdas() = codegen(
        """
            var calledA = false
            fun <T> interceptorFactoryA(): @Interceptor (() -> T) -> () -> T = { calledA = true; it }
            var calledB = false
            fun <T> interceptorFactoryB(): @Interceptor (() -> T) -> () -> T = { calledB = true; it }

            @Module val fooInterceptorModule = moduleOf(
                interceptorFactoryA<Foo>(),
                interceptorFactoryB<Foo>()
            )

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val foo: Foo
            }

            fun invoke(): Pair<Boolean, Boolean> {
                create<MyComponent>().foo
                return calledA to calledB
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Boolean, Boolean>>()
        assertTrue(a)
        assertTrue(b)
    }

    @Test
    fun testMultipleInterceptorLambdas2() = codegen(
        """
            var calledA = false
            typealias InterceptorA<T> = (() -> T) -> () -> T
            fun <T> interceptorFactoryA(): @Interceptor InterceptorA<T> = { calledA = true; it }
            var calledB = false
            typealias InterceptorB<T> = (() -> T) -> () -> T
            fun <T> interceptorFactoryB(): @Interceptor InterceptorB<T> = { calledB = true; it }

            @Module val fooInterceptorModule = moduleOf(
                interceptorFactoryA<Foo>(),
                interceptorFactoryB<Foo>()
            )

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val foo: Foo
            }

            fun invoke(): Pair<Boolean, Boolean> {
                create<MyComponent>().foo
                return calledA to calledB
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Boolean, Boolean>>()
        assertTrue(a)
        assertTrue(b)
    }

    @Test
    fun testModuleLambda() = codegen(
        """
            class Dep<T>(val value: T)
            fun <T> depModuleFactory(): @Module () -> @Binding (T) -> Dep<T> = { { Dep(it) } }
            @Module val fooSetModule: @Module () -> @Module () -> @Binding (Foo) -> Dep<Foo> = { depModuleFactory<Foo>() }

            @Binding fun foo() = Foo()

            @Component interface MyComponent {
                val dep: Dep<Foo>
            }
        """
    )

    @Test
    fun testComposableBindingLambda() = codegen(
        """
            val barProvider: @Binding @Composable (Foo) -> Bar = { Bar(it) }
            @Module val barModule = moduleOf(barProvider)

            @Binding fun foo() = Foo()

            fun invoke() = create<Bar>()
        """
    ) {
        assertInternalError("Call context mismatch")
    }

    @Test
    fun testSuspendBindingLambda() = codegen(
        """
            val barProvider: @Binding suspend (Foo) -> Bar = { Bar(it) }
            @Module val barModule = moduleOf(barProvider)

            @Binding fun foo() = Foo()

            fun invoke() = create<Bar>()
        """
    ) {
        assertInternalError("Call context mismatch")
    }

}
