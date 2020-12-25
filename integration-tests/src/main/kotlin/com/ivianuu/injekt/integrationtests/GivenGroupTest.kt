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

}