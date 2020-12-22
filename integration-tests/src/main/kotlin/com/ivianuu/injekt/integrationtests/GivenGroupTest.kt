package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertSame
import org.junit.Test

class GivenGroupTest {

    @Test
    fun testClassGivenGroup() = codegen(
        """
            @Given val foo = Foo()
            @GivenGroup class BarGroup(@Given private val foo: Foo) {
                @Given val bar get() = Bar(foo)
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testObjectGivenGroup() = codegen(
        """
            @Given val foo = Foo()
            @GivenGroup object BarGroup {
                @Given fun bar(@Given foo: Foo) = Bar(foo)
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testGivenGroupLambdaParameter() = codegen(
        """
            class Ctx(@Given val foo: Foo)
            val factory: (@GivenGroup Ctx) -> Foo = { given() }
            fun invoke(foo: Foo) = factory(Ctx(foo))
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
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

}