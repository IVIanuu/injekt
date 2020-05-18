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

import com.ivianuu.injekt.assertCompileError
import com.ivianuu.injekt.codegen
import org.junit.Test

class CompositionDslTest {

    @Test
    fun testCompositionModuleCannotHaveValueParameters() = codegen(
        """
        @Module
        fun module(instance: String) {
            installIn<TestComponent>()
        }
    """
    ) {
        assertCompileError("value parameter")
    }

    @Test
    fun testCompositionModuleCannotHaveTypeParameters() = codegen(
        """
        @Module
        fun <T> module() {
            installIn<TestComponent>()
        }
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testInstallInCanOnlyBeCalledInModules() = codegen(
        """
        @Factory
        fun module() {
            installIn<TestComponent>()
        }
    """
    ) {
        assertCompileError("module")
    }

    @Test
    fun testParentCanOnlyBeCalledInCompositionFactory() = codegen(
        """
        @Module
        fun module() {
            parent<TestComponent>()
        }
    """
    ) {
        assertCompileError("compositionfactory")
    }

}