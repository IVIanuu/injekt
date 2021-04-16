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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.junit.*

class QualifierTest {
    @Test
    fun testDistinctQualifier() = singleAndMultiCodegen(
        """
            @Given val foo = Foo()
            @Given val qualifiedFoo: @Qualifier1 Foo = Foo()
            """,
        """
           fun invoke(): Pair<Foo, Foo> {
                return given<Foo>() to given<@Qualifier1 Foo>()
           } 
        """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        foo1 shouldNotBeSameInstanceAs foo2
    }

    @Test
    fun testTypeParameterWithQualifierUpperBound() = singleAndMultiCodegen(
        """
            @Given class Dep<T>(@Given val value: @Qualifier1 T)
            
            @Given fun qualified(): @Qualifier1 String = ""
            """,
        """
           fun invoke() = given<Dep<String>>() 
        """
    )

    @Test
    fun testQualifiedClass() = singleAndMultiCodegen(
        """ 
            @Given @Qualifier1 class Dep
            """,
        """
            fun invoke() = given<@Qualifier1 Dep>()
        """
    )

    @Test
    fun testQualifiedFunction() = codegen(
        """ 
            @Given @Qualifier1 fun foo() = Foo()
        """
    ) {
        compilationShouldHaveFailed("only types and classes can be annotated with a qualifier")
    }

    @Test
    fun testQualifierWithArguments() = codegen(
        """ 
            @Qualifier annotation class MyQualifier(val value: String)
            """
    ) {
        compilationShouldHaveFailed("qualifier cannot have value parameters")
    }

    @Test
    fun testQualifierWithTypeArguments() = singleAndMultiCodegen(
        """
            @Qualifier annotation class MyQualifier<T>
            @Given val qualifiedFoo: @MyQualifier<String> Foo = Foo()
            """,
        """
           fun invoke() = given<@MyQualifier<String> Foo>() 
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testQualifierWithGenericTypeArguments() = singleAndMultiCodegen(
        """
            @Qualifier annotation class MyQualifier<T>
            @Given fun <T> qualifiedFoo(): @MyQualifier<T> Foo = Foo()
            """,
        """
           fun invoke() = given<@MyQualifier<String> Foo>() 
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testUiState() = codegen(
        """
            @Qualifier annotation class UiState

            @Given fun <T> uiState(@Given instance: @UiState T): T = instance

            @Given val foo: @UiState Foo = Foo()

            fun invoke() = given<Foo>()
            """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testSubstitutesQualifierTypeParameters() = codegen(
        """
            @Given 
            fun foo(): @Eager<AppGivenScope> Foo = Foo()

            typealias ChildGivenScope = DefaultGivenScope

            @Given
            val childGivenScopeModule = ChildGivenScopeModule0<AppGivenScope, ChildGivenScope>()

            @InstallElement<ChildGivenScope>
            @Given
            class MyElement(@Given val foo: Foo)

            fun invoke() {
                val givenScope = given<AppGivenScope>()
            }
        """
    ) {
        compilationShouldBeOk()
        irShouldNotContain("scopedImpl<Foo, Foo, U>(")
    }
}

