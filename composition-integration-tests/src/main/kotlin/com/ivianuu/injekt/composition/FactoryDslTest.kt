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

package com.ivianuu.injekt.composition

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class FactoryDslTest {

    @Test
    fun testCannotInvokeCompositionFactories() = codegen(
        """
            @CompositionFactory
            fun factory(): TestComponent = create()
            
            fun invoke() = factory()
    """
    ) {
        assertCompileError("cannot invoke")
    }

    @Test
    fun testCanReferenceCompositonFactories() = codegen(
        """
            @CompositionFactory
            fun factory(): TestCompositionComponent = create()
            
            fun invoke() = ::factory
    """
    ) {
        assertOk()
    }

    @Test
    fun testCompositionFactoryCannotBeInline() = codegen(
        """
        @CompositionFactory
        inline fun factory(): TestComponent = create()
    """
    ) {
        assertCompileError("inline")
    }

    @Test
    fun testCompositionFactoryCannotBeSuspend() =
        codegen(
            """
        @CompositionFactory 
        suspend fun factory(): TestComponent = create()
        """
        ) {
            assertCompileError("suspend")
        }

    @Test
    fun testCreateInACompositionFactory() =
        codegen(
            """
        @CompositionFactory fun factory() = create<TestCompositionComponent>()
    """
        ) {
            assertOk()
        }

    @Test
    fun testCompositionFactoryCannotHaveTypeParameters() =
        codegen(
            """
        @CompositionFactory
        inline fun <T> factory(): TestComponent = create()
    """
        ) {
            assertCompileError("type parameter")
        }


}