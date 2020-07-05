/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.frontend

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class FactoryDslTest {

    @Test
    fun testCreateInAFactory() =
        codegen(
            """
        @Factory fun factory() = create<TestComponent>()
    """
        ) {
            assertOk()
        }

    @Test
    fun testCreateInAChildFactory() =
        codegen(
            """
        @ChildFactory fun factory() = create<TestComponent>()
    """
        ) {
            assertOk()
        }

    @Test
    fun testCreateCannotBeCalledOutsideOfAFactory() =
        codegen(
            """
        fun nonFactory() = create<TestComponent>()
    """
        ) {
            assertCompileError("factory")
        }

    @Test
    fun testFactoryWithCreateExpression() = codegen(
        """
        @Factory
        fun exampleFactory(): TestComponent = create()
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
            return create()
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
            return create()
        }
    """
        ) {
            assertOk()
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
    fun testFactoryWithAbstractClassWithZeroArgsConstructor() =
        codegen(
            """
        abstract class Impl
        @Factory
        fun factory(): Impl = create()
    """
        ) {
            assertOk()
        }

    @Test
    fun testFactoryWithAbstractClassWithConstructorParams() =
        codegen(
            """
        abstract class Impl(p0: String)
        @Factory
        fun factory(): Impl = create()
    """
        ) {
            assertCompileError("empty")
        }

    @Test
    fun testFactoryWithInterface() = codegen(
        """
        interface Impl
        @Factory
        fun factory(): Impl = create()
    """
    ) {
        assertOk()
    }

    @Test
    fun testFactoryWithNormalClass() = codegen(
        """
        class Impl
        @Factory
        fun factory(): Impl = create()
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testMutablePropertiesNotAllowedInFactoryImpls() =
        codegen(
            """
        interface Impl {
            var state: String
        }
        @Factory
        fun <T> factory(): Impl = create()
    """
        ) {
            assertCompileError("mutable")
        }

    @Test
    fun testValueParametersNotAllowedInProvisionFunctions() =
        codegen(
            """
        interface Impl {
            fun provisionFunction(p0: String): String
        }
        @Factory
        fun factory(): Impl = create()
    """
        ) {
            assertCompileError("parameters")
        }

    @Test
    fun testSuspendValueNotAllowedInProvisionFunctions() =
        codegen(
            """
        interface Impl {
            suspend fun provisionFunction(): String
        }
        @Factory
        fun factory(): Impl = create()
    """
        ) {
            assertCompileError("suspend")
        }

    @Test
    fun testCannotInvokeChildFactories() = codegen(
        """
            @ChildFactory
            fun factory(): TestComponent = create()
            
            fun invoke() = factory()
    """
    ) {
        assertCompileError("cannot invoke")
    }

    @Test
    fun testCanReferenceChildFactories() = codegen(
        """
            @ChildFactory
            fun factory(): TestComponent = create()
            
            fun invoke() = ::factory
    """
    ) {
        assertOk()
    }

    @Test
    fun testFactoryAndModule() = codegen(
        """
        @ChildFactory
        @Factory 
        @Module
        fun factory(): TestComponent = create()
        """
    ) {
        assertCompileError("only")
    }

    @Test
    fun testFactoryCannotBeSuspend() = codegen(
        """
        @Factory 
        suspend fun factory(): TestComponent = create()
        """
    ) {
        assertCompileError("suspend")
    }

    @Test
    fun testFactoryCannotBeInline() = codegen(
        """
        @Factory
        inline fun factory(): TestComponent = create()
    """
    ) {
        assertCompileError("inline")
    }

    @Test
    fun testFactoryCannotHaveTypeParameters() = codegen(
        """
        @Factory 
        fun <T> factory(): TestComponent = create()
    """
    ) {
        assertCompileError("type")
    }

}
