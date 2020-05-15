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
