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

    @Test
    fun testFactoryWithCreateExpression() = codegen(
        """
        @Factory
        fun exampleFactory(): TestComponent = createImplementation()
    """
    ) {
        assertOk()
    }

    @Test
    fun testFactoryWithReturnCreateExpression() = codegen(
        """
        @Factory
        fun exampleFactory(): TestComponent {
            return createImplementation()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testFactoryWithMultipleStatements() = codegen(
        """
        @Factory
        fun exampleFactory(): TestComponent {
            println()
            return createImplementation()
        }
    """
    ) {
        assertCompileError("statement")
    }

    @Test
    fun testFactoryWithoutCreate() = codegen(
        """
        @Factory
        fun exampleFactory() {
        }
    """
    ) {
        assertCompileError("statement")
    }

}
