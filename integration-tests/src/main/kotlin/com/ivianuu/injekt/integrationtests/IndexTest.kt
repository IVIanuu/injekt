package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class IndexTest {

    @Test
    fun testCanIndexDeclarationsWithTheSameName() = codegen(
        """
            @Given val foo get() = Foo()
            
            @Given fun foo() = Foo()
        """
    )

}
