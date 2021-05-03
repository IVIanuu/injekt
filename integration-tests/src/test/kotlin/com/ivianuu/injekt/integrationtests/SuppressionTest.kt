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
    fun testTypeAliasTypeParameter() = codegen(
        """
            typealias Alias<T> = String
        """
    ) {
        shouldNotContainMessage("Type alias parameter T is not used in the expanded type String and does not affect type checking")
    }

    @Test
    fun testCanUseExtensionFunctionTypeUpperBound() = singleAndMultiCodegen(
        """
            typealias MyBuilder = StringBuilder.() -> Unit
            @Given fun <@Given T : MyBuilder> toString(@Given builder: MyBuilder): String = buildString(builder)
            @Given val myBuilder: MyBuilder = { append("42") }
        """,
        """
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
    fun testCanUseUnderscoreForGivenParameter() = singleAndMultiCodegen(
        """
            fun func(@Given _: String, @Given _: Int) {
                given<String>()
                given<Int>()
            }
        """,
        """
            fun invoke() {
                @Given val string = ""
                func(int = 0)
            } 
        """
    )

    @Test
    fun testCanUseInfixWithGiven() = singleAndMultiCodegen(
        """
            interface Combine<T> {
                fun plus(a: T, b: T): T
            }

            infix fun <T> T.combine(other: T, @Given combine: Combine<T>): T = combine.plus(this, other)
            
            @Given object StringCombine : Combine<String> {
                override fun plus(a: String, b: String) = a + b
            }
        """,
        """
            fun invoke() {
                "a" combine "b"
            } 
        """
    )

    @Test
    fun testCanUseOperatorWithGiven() = singleAndMultiCodegen(
        """
            interface Combine<T> {
                fun plus(a: T, b: T): T
            }

            operator fun <T> T.plus(other: T, @Given combine: Combine<T>): T = combine.plus(this, other)

            inline class Key(val value: String)

            @Given object KeyCombine : Combine<Key> {
                override fun plus(a: Key, b: Key) = Key(a.value + b.value)
            }
        """,
        """
            fun invoke() {
                Key("a") + Key("b")
            } 
        """
    )

    @Test
    fun testUsedGivenParameterIsNotMarkedAsUnused() = codegen(
        """
            fun func1(@Given foo: Foo) {
                func2()                
            }

            fun func2(@Given foo: Foo) {
                foo
            }
        """
    ) {
        shouldNotContainMessage("Parameter 'foo' is never used")
    }

    @Test
    fun testUnusedGivenParameterIsMarkedAsUnused() = codegen(
        """
            fun func1(@Given foo: Foo) {
            }

            fun func2(@Given foo: Foo) {
                foo
            } 
        """
    ) {
        shouldContainMessage("Parameter 'foo' is never used")
    }

    @Test
    fun testUsedGivenVariableIsNotMarkedAsUnused() = codegen(
        """
            fun invoke() {
                @Given val foo = Foo()
                given<Foo>()
            }
        """
    ) {
        shouldNotContainMessage("Variable 'foo' is never used")
    }

    @Test
    fun testUnusedGivenVariableIsMarkedAsUnused() = codegen(
        """
            fun invoke() {
                @Given val foo = Foo()
            }
        """
    ) {
        shouldContainMessage("Variable 'foo' is never used")
    }
}
