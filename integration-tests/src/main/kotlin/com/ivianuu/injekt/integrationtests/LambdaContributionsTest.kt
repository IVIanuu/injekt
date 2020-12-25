package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert
import junit.framework.Assert.assertTrue
import org.junit.Test

class LambdaContributionsTest {

    @Test
    fun testGivenGroupLambdaParameter() = codegen(
        """
            class Ctx(@Given val foo: Foo)
            val factory: (@GivenGroup Ctx) -> Foo = { given() }
            fun invoke(foo: Foo) = factory(Ctx(foo))
        """
    ) {
        val foo = Foo()
        Assert.assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testLambdaGivenGroup() = codegen(
        """
            @Given val foo = Foo()
            @GivenGroup val barGiven: @Given (@Given Foo) -> Bar = { Bar(it) }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testNestedLambdaGivenGroup() = codegen(
        """
            @Given val foo = Foo()
            @GivenGroup val barGiven: @GivenGroup (@Given Foo) -> @Given () -> Bar = { foo ->
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
            @GivenGroup val fooSet: @GivenSetElement (@Given Foo) -> Foo = { it }
            fun invoke() = given<Set<Foo>>()
        """
    )

    @Test
    fun testInterceptorLambda() = codegen(
        """
            var called = false
            fun <T> interceptorFactory(): @Interceptor (() -> T) -> T = { called = true; it() }
           
            @GivenGroup val fooInterceptorModule = interceptorFactory<Foo>()
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