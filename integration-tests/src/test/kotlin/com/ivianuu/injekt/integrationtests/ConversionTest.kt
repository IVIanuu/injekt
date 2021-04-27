/*
 * Copyright 2021 Manuel Wrage
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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import org.junit.*

class ConversionTest {
    @Test
    fun testConversionWithFunctionExtensionMember() = codegen(
        """
            @Given fun string2CoroutineScope(@Given int: Int): Conversion<String, CoroutineScope> = {
                CoroutineScope(Job())
            }
            @Given val int: Int = 0
            val CoroutineScope.property: Int get() = 0
            fun invoke() = "".property
        """
    ) {
        invokeSingleFile() shouldBe 0
    }

    @Test
    fun testConversionWithFunctionMember() = codegen(
        """
            class MyClass {
                fun foo() {
                }
            }
            @Given fun string2MyClass(@Given int: Int): Conversion<String, MyClass> = {
                MyClass()
            }
            @Given val int: Int = 0
            fun main() {
                "".foo()
            }
        """
    )

    @Test
    fun testConversionWithPropertyExtensionMember() = codegen(
        """
            @Given fun string2CoroutineScope(@Given int: Int): Conversion<String, CoroutineScope> = {
                CoroutineScope(Job())
            }
            @Given val int: Int = 0
            val CoroutineScope.property: Int get() = 0
            fun invoke() = "".property
        """
    ) {
        invokeSingleFile() shouldBe 0
    }

    @Test
    fun testConversionWithPropertyMember() = codegen(
        """
            class MyClass {
                val foo = Foo()
            }
            @Given fun string2MyClass(@Given int: Int): Conversion<String, MyClass> = {
                MyClass()
            }
            @Given val int: Int = 0
            fun main() {
                "".foo
            }
        """
    )
}