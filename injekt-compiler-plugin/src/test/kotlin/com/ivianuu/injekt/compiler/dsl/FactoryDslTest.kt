package com.ivianuu.injekt.compiler.dsl

import com.ivianuu.injekt.compiler.assertCompileError
import com.ivianuu.injekt.compiler.assertOk
import com.ivianuu.injekt.compiler.codegen
import org.junit.Test

class FactoryDslTest {

    @Test
    fun testCreateImplementationInAFactory() =
        codegen(
            """
        @Factory fun factory() = createImplementation<TestComponent>()
    """
        ) {
            assertOk()
        }

    @Test
    fun testCreateImplementationInAChildFactory() =
        codegen(
            """
        @ChildFactory fun factory() = createImplementation<TestComponent>()
    """
        ) {
            assertOk()
        }

    @Test
    fun testCreateImplementationCannotBeCalledOutsideOfAFactory() =
        codegen(
            """
        fun nonFactory() = createImplementation<TestComponent>()
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
    fun testFactoryWithReturnCreateExpression() =
        codegen(
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
    fun testFactoryWithMultipleStatements() =
        codegen(
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

    @Test
    fun testFactoryWithAbstractClass() = codegen(
        """
        abstract class Impl
        @Factory
        fun factory(): Impl = createImplementation()
    """
    ) {
        assertOk()
    }

    @Test
    fun testFactoryWithInterface() = codegen(
        """
        interface Impl
        @Factory
        fun factory(): Impl = createImplementation()
    """
    ) {
        assertOk()
    }

    @Test
    fun testFactoryWithNormalClass() = codegen(
        """
        class Impl
        @Factory
        fun factory(): Impl = createImplementation()
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testFactoryWithTypeParameters() = codegen(
        """
        class Impl
        @Factory
        fun <T> factory(): Impl = createImplementation()
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testMutablePropertiesNotAllowedInFactoryImpls() =
        codegen(
            """
        interface Impl {
            var state: String
        }
        @Factory
        fun <T> factory(): Impl = createImplementation()
    """
        ) {
            assertCompileError("mutable")
        }

    @Test
    fun testParametersNotAllowedInProvisionFunctions() =
        codegen(
            """
        interface Impl {
            fun provisionFunction(p0: String): String
        }
        @Factory
        fun <T> factory(): Impl = createImplementation()
    """
        ) {
            assertCompileError("parameters")
        }

    @Test
    fun testStaticFactoryOk() = codegen(
        """
        @Factory
        fun factory(): TestComponent = createImplementation()
    """
    ) {
        assertOk()
    }

    @Test
    fun testStaticFactoryInObjectOk() = codegen(
        """
        object Object {
            @Factory
            fun factory(): TestComponent = createImplementation()
        }
    """
    ) {
        assertOk()
    }

    @Test
    fun testNonStaticFactoryFails() = codegen(
        """
        class Class {
            @Factory
            fun factory(): TestComponent = createImplementation()
        }
    """
    ) {
        assertCompileError("static")
    }

}
