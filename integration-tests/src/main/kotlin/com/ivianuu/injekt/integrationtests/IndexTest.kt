package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class IndexTest {

    @Test
    fun testCanIndexDeclarationsWithTheSameNameInTheSameFile() = codegen(
        """
            @Given val foo get() = Foo()
            
            @Given fun foo() = Foo()
        """
    )

    @Test
    fun testCanIndexDeclarationsWithTheSameNameInTheSamePackage() = codegen(
        source(
            """
                    @Given val foo get() = Foo()
                """
        ),
        source(
            """
                    @Given fun foo() = Foo()
                """
        )
    )

}
