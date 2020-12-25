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

    // todo test order

    // todo test no factory parameter

    // todo test missing interceptor dependency

    // todo test suspend interceptor
    // todo test composable interceptor
}
