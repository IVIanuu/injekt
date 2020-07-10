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

package com.ivianuu.injekt

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class FrontendComponentTest {

    @Test
    fun testComponentWithTypeParameterFails() = codegen(
        """
        @Component
        interface MyComponent<T>
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testNonEmptyComponentFails() = codegen(
        """
        @Component
        interface MyComponent<T> {
            fun t(): T
        }
    """
    ) {
        assertCompileError("empty")
    }

    @Test
    fun testNonInterfaceComponentFails() = codegen(
        """
        @Component
        class MyComponent
    """
    ) {
        assertCompileError("interface")
    }

    @Test
    fun testNonInterfaceComponentFactoryFails() = codegen(
        """
        @Component.Factory
        class MyComponent
    """
    ) {
        assertCompileError("interface")
    }

    @Test
    fun testComponentFactoryWithTypeParameterFails() = codegen(
        """
        @Component.Factory
        interface MyComponentFactory<T> {
            fun create(): T
        }
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testComponentFactoryWithoutSingleFunctionFails() = codegen(
        """
        @Component
        interface MyComponent {
            @Component.Factory
            interface Factory
        }
    """
    ) {
        assertCompileError("single function")
    }

    @Test
    fun testComponentFactoryWithWrongReturnTypeFails() = codegen(
        """
        @Component
        interface MyComponent {
            @Component.Factory
            interface Factory {
                fun create(): Any
            }
        }
    """
    ) {
        assertCompileError("single function")
    }

}