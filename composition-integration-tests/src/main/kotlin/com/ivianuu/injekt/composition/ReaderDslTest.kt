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

class ReaderDslTest {

    @Test
    fun testReaderInvocationInReaderAllowed() =
        codegen(
            """
            @Reader fun a() {}
            @Reader fun b() { a() }
        """
        ) {
            assertOk()
        }

    @Test
    fun testReaderInvocationInNonReaderNotAllowed() =
        codegen(
            """
            @Reader fun a() {}
            fun b() { a() }
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testReaderInvocationInNonReaderLambdaIsNotAllowed() =
        codegen(
            """
            val lambda: () -> Unit = {
                func()
            }
            @Reader fun func() {}
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testNestedReaderInvocationInReaderAllowed() =
        codegen(
            """
            @Reader fun a() {}
            fun b(block: () -> Unit) = block()
            @Reader
            fun c() {
                b {
                    a()
                }
            }
        """
        ) {
            assertOk()
        }

    @Test
    fun testReaderClassConstructionInReaderAllowed() =
        codegen(
            """
            @Reader class ReaderClass
            @Reader fun b() { ReaderClass() }
        """
        ) {
            assertOk()
        }

    @Test
    fun testReaderClassConstructionInReaderNotAllowed() =
        codegen(
            """
            @Reader class ReaderClass
            fun b() { ReaderClass() }
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testOpenReaderFails() = codegen(
        """
        open class MyClass {
            @Reader 
            open fun func() {
            }
        }
        """
    ) {
        assertCompileError("final")
    }

    @Test
    fun testReaderInterfaceFails() = codegen(
        """
        @Reader
        interface Interface
    """
    ) {
        assertCompileError("interface")
    }

    @Test
    fun testReaderObjectFails() = codegen(
        """
        @Reader
        object Object
    """
    ) {
        assertCompileError("object")
    }

}
