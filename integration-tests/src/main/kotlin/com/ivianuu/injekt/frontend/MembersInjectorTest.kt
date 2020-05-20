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

class MembersInjectorTest {

    @Test
    fun testInjectFunctionWithReturnType() = codegen(
        """
        class MyClass {
            @Inject
            fun inject(): String = ""
        }
    """
    ) {
        assertCompileError("return")
    }

    @Test
    fun testNonMemberInjectFunction() = codegen(
        """
        @Inject 
        fun inject() {
        }
    """
    ) {
        assertCompileError("member")
    }

    @Test
    fun testInjectFunctionInInterface() = codegen(
        """
        interface MyClass {
            @Inject
            fun inject()
        }
    """
    ) {
        assertCompileError("member")
    }

    @Test
    fun testInjectFunctionWithTypeParameters() = codegen(
        """
        class MyClass {
            @Inject
            fun <T> inject() {
            }
        }
    """
    ) {
        assertCompileError("type param")
    }

    @Test
    fun testInjectFunctionCannotBeAbstract() = codegen(
        """
        class MyClass {
            @Inject
            abstract fun <T> inject()
        }
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testOpenInjectProperty() = codegen(
        """
        open class MyClass { 
            open val foo: Foo by inject()
        }
    """
    ) {
        assertCompileError("final")
    }

    @Test
    fun testInjectPropertyWithExtensionReceiver() = codegen(
        """
        class MyClass { 
            val Bar.foo: Foo by inject()
        }
    """
    ) {
        assertCompileError("extension")
    }

    @Test
    fun testTopLevelProperty() = codegen(
        """
        val Bar.foo: Foo by inject()
    """
    ) {
        assertCompileError("class")
    }

}
