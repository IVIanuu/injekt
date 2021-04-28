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

class SuppressionTest {
    @Test
    fun testDoesNotWarnFinalUpperBound() = codegen(
        """
            fun <T : Int> func() {
            }
        """
    ) {
        shouldNotContainMessage("'Int' is a final type, and thus a value of the type parameter is predetermined")
    }

    @Test
    fun testCanUseExtensionFunctionTypeUpperBound() = codegen(
        """
            typealias MyBuilder = StringBuilder.() -> Unit
            @Given fun <@Given T : MyBuilder> toString(@Given builder: MyBuilder): String = buildString(builder)
            @Given val myBuilder: MyBuilder = { append("42") }
            fun invoke() = given<String>()
        """
    ) {
        invokeSingleFile() shouldBe "42"
    }

    @Test
    fun testDoesNotWarnInlineOnGivenDeclaration() = codegen(
        """
            @Given inline fun func() {
            }
        """
    ) {
        shouldNotContainMessage("Expected performance impact from inlining is insignificant. Inlining works best for functions with parameters of functional types")
    }

    @Test
    fun testCanUseUnderscoreForGivenParameter() = codegen(
        """
            @Given fun func(@Given _: String) {
            }
        """
    )
}
