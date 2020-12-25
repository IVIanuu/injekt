package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import org.junit.Test

class InterceptorTest {

    @Test
    fun testInterceptor() = codegen(
        """
            var callCount = 0
            @Interceptor fun <T> intercept(factory: () -> T): T { 
                callCount = callCount + 1
                return factory()
            }
            
            @Given fun foo() = Foo()
            
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            
            @Given fun baz(@Given foo: Foo, @Given bar: Bar) = Baz(bar, foo)
            
            fun invoke(): Int {
                given<Baz>()
                return callCount
            }
        """
    ) {
        assertEquals(4, invokeSingleFile<Int>())
    }

    @Test
    fun testInterceptorWithDependencies() = codegen(
        """
            @Interceptor fun intercept(@Given foo: Foo, factory: () -> Bar): Bar =
                factory()

            @Given fun foo() = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke() = given<Bar>()
        """
    )

    @Test fun testInterceptorWithoutFactoryParameter() = codegen(
        """
            @Interceptor fun intercept() = Unit
        """
    ) {
        assertCompileError("@Interceptor declaration must have one parameter")
    }

    @Test fun testInterceptorLambdaWithoutFactoryParameter() = codegen(
        """
            val intercept: @Interceptor () -> Unit = {}
        """
    ) {
        assertCompileError("@Interceptor declaration must have one parameter")
    }

    @Test fun testInterceptorOrder() = codegen(
        """
            val calls = mutableListOf<String>()

            @Interceptor fun interceptA(factory: () -> Foo): Foo {
                calls += "a"
                return factory()
            }

            @Interceptor fun interceptB(factory: () -> Foo): Foo {
                calls += "b"
                return factory()
            }

            @Given fun foo() = Foo()

            fun invoke(): List<String> {
                given<Foo>()
                return calls
            }
        """
    ) {
        assertEquals(
            listOf("a", "b"),
            invokeSingleFile()
        )
    }

    @Test fun testInterceptorLambdaParameter() = codegen(
        """
            var callCount = 0
            inline fun <R> withFooInterceptor(
                block: (@Interceptor (() -> Foo) -> Foo) -> R
            ): R = block {
                callCount = callCount + 1
                it()
            }

            @Given fun foo() = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke(): Int = withFooInterceptor { 
                given<Bar>()
                callCount
            }
        """
    ) {
        assertEquals(1, invokeSingleFile())
    }

    @Test fun testInterceptorInModule() = codegen(
        """
            var callCount = 0

            class MyModule {
                @Interceptor fun <T> intercept(factory: () -> T): T {
                    callCount += 1
                    return factory()
                }
            }

            inline fun <R> withModule(
                block: (@Module MyModule) -> R
            ): R = block(MyModule())

            @Given fun foo() = Foo()

            fun invoke(): Int = withModule { 
                given<Foo>()
                callCount 
            }
        """
    ) {
        assertEquals(1, invokeSingleFile())
    }
}
