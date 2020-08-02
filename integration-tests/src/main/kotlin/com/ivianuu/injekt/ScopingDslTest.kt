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

class ScopingDslTest {

    @Test
    fun testCorrectScopingOk() = codegen(
        """
            @Scoping
            object MyScoping {
                @Reader
                fun <T> scope(key: Any, init: () -> T) = init()
            }
        """
    )

    @Test
    fun testNotObjectScopingFails() = codegen(
        """
            @Scoping
            class MyScoping {
                @Reader
                fun <T> scope(key: Any, init: () -> T) = init()
            }
        """
    ) {
        assertCompileError("object")
    }

    @Test
    fun testScopingWithoutFunctionFails() = codegen(
        """
            @Scoping
            object MyScoping {

            }
        """
    ) {
        assertCompileError("function")
    }

    @Test
    fun testScopingWithoutTypeParameterFails() = codegen(
        """
            @Scoping
            object MyScoping {
                @Reader
                fun scope(key: Any, init: () -> Any) = init()
            }
        """
    ) {
        assertCompileError("function")
    }

    @Test
    fun testScopingWithoutKeyParameterFails() = codegen(
        """
            @Scoping
            object MyScoping {
                @Reader
                fun <T> scope(init: () -> T) = init()
            }
        """
    ) {
        assertCompileError("function")
    }

    @Test
    fun testScopingWithoutInitParameterFails() = codegen(
        """
            @Scoping
            object MyScoping {
                @Reader
                fun <T> scope(key: Any) = init()
            }
        """
    ) {
        assertCompileError("function")
    }

    @Test
    fun testScopingWithWrongReturnTypeFails() = codegen(
        """
            @Scoping
            object MyScoping {
                @Reader
                fun <T : Any> scope(key: Any, init: () -> T): Any = init()
            }
        """
    ) {
        assertCompileError("function")
    }

}
