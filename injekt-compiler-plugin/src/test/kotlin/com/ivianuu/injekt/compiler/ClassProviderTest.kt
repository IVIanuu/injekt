package com.facebook.buck.jvm.java.javax.com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.assertOk
import com.ivianuu.injekt.compiler.codegen
import org.junit.Test

class ClassProviderTest {

    @Test
    fun test() = codegen(
        """
        @Factory
        class MyClass
    """
    ) {
        assertOk()
    }

}