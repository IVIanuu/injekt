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
    fun testDoesNotOptimizeNormalClass() = singleAndMultiCodegen(
        """
            class MyModule
            @Given val foo = Foo()
        """,
        """
           fun invoke() = given<Foo>() 
        """
    ) {
        irShouldNotContain("INSTANCE")
        invokeSingleFile()
    }

    @Test
    fun testDoesNotOptimizeObject() = singleAndMultiCodegen(
        """
            @Given object MyModule {
                @Given val foo = Foo()
            }
        """,
        """
           fun invoke() = given<Foo>() 
        """
    ) {
        irShouldNotContain("INSTANCE")
        invokeSingleFile()
    }

    @Test
    fun testDoesNotOptimizeGivenWithConstructorParameters() = singleAndMultiCodegen(
        """
            @Given class MyModule(@Given val foo: Foo)
            @Given val foo = Foo()
        """,
        """
           fun invoke() = given<Foo>() 
        """
    ) {
        irShouldNotContain("INSTANCE")
        invokeSingleFile()
    }

    @Test
    fun testDoesNotOptimizeGivenWithForTypeKeyParameters() = singleAndMultiCodegen(
        """
            @Given class MyModule<@ForTypeKey T> {
                @Given val instance = Foo() as T
            }
        """,
        """
           fun invoke() = given<Foo>() 
        """
    ) {
        irShouldNotContain("INSTANCE")
        invokeSingleFile()
    }

    @Test
    fun testDoesNotOptimizeGivenWithFields() = singleAndMultiCodegen(
        """
            @Given class MyModule {
                @Given val foo = Foo()
            }
        """,
        """
           fun invoke() = given<Foo>() 
        """
    ) {
        irShouldNotContain("INSTANCE")
        invokeSingleFile()
    }

    @Test
    fun testDoesNotOptimizeGivenWithInnerClass() = singleAndMultiCodegen(
        """
            @Given class MyModule {
                inner class Inner
            }
            @Given val foo = Foo()
        """,
        """
           fun invoke() = given<Foo>() 
        """
    ) {
        irShouldNotContain("INSTANCE")
        invokeSingleFile()
    }

    @Test
    fun testDoesOptimizeGivenWithComputedProperties() = singleAndMultiCodegen(
        """
            @Given class MyModule {
                @Given val foo get() = Foo()
            }
        """,
        """
           fun invoke() = given<Foo>() 
        """
    ) {
        irShouldContain(if (!it) 2 else 1, "INSTANCE")
        invokeSingleFile()
    }
}
