package com.ivianuu.injekt.compiler

import org.junit.Test

class FactoryTest {

    @Test
    fun testFactoryBlockMovesToAFunction() = codegen(
        """
        @Factory
        fun exampleFactory() {
        }
    """
    ) {
        assertOk()
    }

}