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
import io.kotest.matchers.types.*
import org.junit.*

class SingletonGivenTest {
    @Test
    fun testSingletonGiven() = singleAndMultiCodegen(
        """
            @Given class MyModule {
                @Given fun foo() = Foo()
            }
        """,
        """
           fun invoke() = given<MyModule>()
        """
    ) {
        invokeSingleFile()
            .shouldBeSameInstanceAs(invokeSingleFile())
    }

    @Test
    fun testDoesNotOptimizeNormalClass() = codegen(
        """
            class MyModule
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeObject() = codegen(
        """
            @Given object MyModule {
                @Given val foo = Foo()
            }
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeGivenWithConstructorParameters() = codegen(
        """
            @Given class MyModule(@Given val foo: Foo)
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeGivenWithForTypeKeyParameters() = codegen(
        """
            @Given class MyModule<@ForTypeKey T> {
                @Given val instance = Foo() as T
            }
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeGivenWithFields() = codegen(
        """
            @Given class MyModule {
                @Given val foo = Foo()
            }
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesNotOptimizeGivenWithInnerClass() = codegen(
        """
            @Given class MyModule {
                inner class Inner
            }
            @Given val foo = Foo()
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldNotContain("var INSTANCE: MyModule")
    }

    @Test
    fun testDoesOptimizeGivenWithComputedProperties() = codegen(
        """
            @Given class MyModule {
                @Given val foo get() = Foo()
            }
            fun invoke() = given<Foo>()
        """
    ) {
        irShouldContain(1, "var INSTANCE: MyModule")
    }
}
