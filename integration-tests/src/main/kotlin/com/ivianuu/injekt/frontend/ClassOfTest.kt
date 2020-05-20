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
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ClassOfTest {

    @Test
    fun testClassOfInNonModule() = codegen(
        """
        fun <T> module() { classOf<T>() }
    """
    ) {
        assertCompileError("module")
    }

    @Test
    fun testClassOfInNonInlineModule() = codegen(
        """
        @Module fun <T> module() { classOf<T>() }
    """
    ) {
        assertCompileError("inline")
    }

    @Test
    fun testClassOfWithReified() = codegen(
        """
        @Module inline fun <reified T> module() { classOf<T>() }
    """
    ) {
        assertCompileError("reified")
    }

    @Test
    fun testClassOfWithConcreteType() = codegen(
        """
        @Module inline fun module() { classOf<String>() }
    """
    ) {
        assertCompileError("generic")
    }

}