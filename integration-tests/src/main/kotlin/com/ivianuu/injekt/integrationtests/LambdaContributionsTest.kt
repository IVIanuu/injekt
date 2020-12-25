package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert
import junit.framework.Assert.assertTrue
import org.junit.Test

class LambdaContributionsTest {

    @Test
    fun testModuleLambdaParameter() = codegen(
        """
            class Ctx(@Given val foo: Foo)
            val factory: (@Module Ctx) -> Foo = { given() }
            fun invoke(foo: Foo) = factory(Ctx(foo))
        """
    ) {
        val foo = Foo()
        Assert.assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testLambdaModule() = codegen(
        """
            @Given val foo = Foo()
            @Module val barGiven: @Given (@Given Foo) -> Bar = { Bar(it) }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testNestedLambdaModule() = codegen(
        """
            @Given val foo = Foo()
            @Module val barGiven: @Module (@Given Foo) -> @Given () -> Bar = { foo ->
                {
                    Bar(foo)
                }
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testLambdaGivenSetElement() = codegen(
        """
            @Given val foo = Foo()
            @Module val fooSet: @GivenSetElement (@Given Foo) -> Foo = { it }
            fun invoke() = given<Set<Foo>>()
        """
    )

    @Test
    fun testInterceptorLambda() = codegen(
        """
            var called = false
            fun <T> interceptorFactory(): @Interceptor (() -> T) -> T = { called = true; it() }
           
            @Module val fooInterceptorModule = interceptorFactory<Foo>()
            @Given fun foo() = Foo()
            
            fun invoke(): Boolean {
                given<Foo>()
                return called
            }
        """
    ) {
        assertTrue(invokeSingleFile<Boolean>())
    }

}