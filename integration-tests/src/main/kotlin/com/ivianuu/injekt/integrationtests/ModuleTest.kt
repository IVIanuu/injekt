package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert
import junit.framework.Assert.assertSame
import org.junit.Test

class ModuleTest {

    @Test
    fun testClassModule() = codegen(
        """
            @Given val foo = Foo()
            @Module class BarModule(@Given private val foo: Foo) {
                @Given val bar get() = Bar(foo)
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testObjectModule() = codegen(
        """
            @Given val foo = Foo()
            @Module object BarModule {
                @Given fun bar(@Given foo: Foo) = Bar(foo)
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test fun testModuleLambdaParameter() = codegen(
        """
            class MyModule {
                @Given val foo = Foo()
            }

            @Given fun foo() = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)

            inline fun <R> withModule(
                block: (@Module MyModule) -> R
            ): R = block(MyModule())

            fun invoke() = withModule { 
                given<Bar>()
            }
        """
    )
}
