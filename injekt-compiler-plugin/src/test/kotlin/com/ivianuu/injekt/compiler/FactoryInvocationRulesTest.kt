package com.ivianuu.injekt.compiler

import org.junit.Test

class FactoryInvocationRulesTest {

    @Test
    fun testCreateImplementationInAFactory() = codegen(
        """
        @Factory fun factory() = createImplementation<Unit>()
    """
    ) {
        assertOk()
    }

    @Test
    fun testCreateImplementationInAChildFactory() = codegen(
        """
        @ChildFactory fun factory() = createImplementation<Unit>()
    """
    ) {
        assertOk()
    }

    @Test
    fun testCreateImplementationCannotBeCalledOutsideOfAFactory() = codegen(
        """
        fun nonFactory() = createImplementation<Unit>()
    """
    ) {
        assertCompileError("factory")
    }

}