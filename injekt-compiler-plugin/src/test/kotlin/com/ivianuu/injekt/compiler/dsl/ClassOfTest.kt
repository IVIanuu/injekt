package com.ivianuu.injekt.compiler.dsl

import com.ivianuu.injekt.compiler.assertCompileError
import com.ivianuu.injekt.compiler.codegen
import org.junit.Test

class ClassOfTest {

    @Test
    fun testClassOfInNonModule() = codegen(
        """
        fun nonModule() { classOf<String>() }
    """
    ) {
        assertCompileError("module")
    }

}