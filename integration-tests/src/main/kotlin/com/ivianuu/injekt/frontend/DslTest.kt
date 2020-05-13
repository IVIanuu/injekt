package com.ivianuu.injekt.frontend

import com.ivianuu.injekt.assertCompileError
import com.ivianuu.injekt.codegen
import org.junit.Test

class DslTest {

    @Test
    fun testDslFunctionInModule() = codegen(
        """
        @Module
        fun module() {
            instance(42)
        }
    """
    )

    @Test
    fun testDslFunctionInFactory() = codegen(
        """
        @Factory
        fun module(): Int {
            instance(42)
            return createInstance()
        }
    """
    )

    @Test
    fun testDslFunctionInNormalFunction() = codegen(
        """
        fun func() {
            instance(42)
        }
    """
    ) {
        assertCompileError("module")
    }

}
