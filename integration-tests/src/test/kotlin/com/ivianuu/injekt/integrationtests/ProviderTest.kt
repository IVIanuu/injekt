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

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

class ProviderTest {

    @Test
    fun testProviderGiven() = codegen(
        """
            @Given val foo = Foo()
            fun invoke(): Foo {
                return given<() -> Foo>()()
            }
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testCannotRequestProviderForNonExistingGiven() = codegen(
        """ 
            fun invoke(): Foo {
                return given<() -> Foo>()()
            }
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type kotlin.Function0<com.ivianuu.injekt.test.Foo> for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testProviderWithGivenArgs() = codegen(
        """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            fun invoke() = given<(@Given Foo) -> Bar>()(Foo())
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Bar>()
    }

    @Test
    fun testProviderWithQualifiedGivenArgs() = codegen(
        """
            @Qualifier annotation class MyQualifier
            @Given fun bar(@Given foo: @MyQualifier Foo) = Bar(foo)
            fun invoke() = given<(@Given @MyQualifier Foo) -> Bar>()(Foo())
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Bar>()
    }

    @Test
    fun testProviderWithGenericGivenArgs() = codegen(
        """ 
            typealias GivenScopeA = GivenScope

            fun createGivenScopeA() = GivenScopeBuilder<GivenScopeA>(initializers = { emptySet() })
                .build()

            typealias GivenScopeB = GivenScope

            @GivenScopeElementBinding<GivenScopeA>
            @Given
            fun givenScopeBFactory(
                @Given parent: GivenScopeA,
                @Given builderFactory: () -> GivenScope.Builder<GivenScopeB>
            ): () -> GivenScopeB = { 
                builderFactory()
                    .dependency(parent)
                    .build()
            }

            typealias GivenScopeC = GivenScope

            @GivenScopeElementBinding<GivenScopeB>
            @Given 
            fun givenScopeCFactory(
                @Given parent: GivenScopeB,
                @Given builderFactory: () -> GivenScope.Builder<GivenScopeC>
            ): () -> GivenScopeC = {
                builderFactory()
                    .dependency(parent)
                    .build()
            }

            @GivenScopeElementBinding<GivenScopeC>
            @Given class MyGivenScope(
                @Given val a: GivenScopeA,
                @Given val b: GivenScopeB,
                @Given val c: GivenScopeC
            )
            """
    )

    @Test
    fun testProviderWithGenericGivenArgsMulti() = multiCodegen(
        listOf(
            source(
                """
                    typealias GivenScopeA = GivenScope
        
                    typealias GivenScopeB = GivenScope
        
                    @GivenScopeElementBinding<GivenScopeA>
                    @Given
                    fun givenScopeBFactory(
                        @Given parent: GivenScopeA,
                        @Given builderFactory: () -> GivenScope.Builder<GivenScopeB>
                    ): () -> GivenScopeB = { 
                        builderFactory()
                            .dependency(parent)
                            .build()
                    }
        
                    typealias GivenScopeC = GivenScope
        
                    @GivenScopeElementBinding<GivenScopeB>
                    @Given 
                    fun givenScopeCFactory(
                        @Given parent: GivenScopeB,
                        @Given builderFactory: () -> GivenScope.Builder<GivenScopeC>
                    ): () -> GivenScopeC = {
                        builderFactory()
                            .dependency(parent)
                            .build()
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    fun createGivenScopeA() = GivenScopeBuilder<GivenScopeA>(initializers = { emptySet() }).build()

                    @GivenScopeElementBinding<GivenScopeC>
                    @Given class MyGivenScope(
                        @Given val a: GivenScopeA,
                        @Given val b: GivenScopeB,
                        @Given val c: GivenScopeC
                    )
                """
            )
        )
    )

    @Test
    fun testProviderModule() = codegen(
        """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            class FooModule(@Given val foo: Foo)
            fun invoke(): Bar {
                return given<(@Given FooModule) -> Bar>()(FooModule(Foo()))
            }
        """
    )

    @Test
    fun testSuspendProviderGiven() = codegen(
        """
            @Given suspend fun foo() = Foo()
            fun invoke(): Foo = runBlocking { given<suspend () -> Foo>()() }
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testComposableProviderGiven() = codegen(
        """
            @Given val foo: Foo @Composable get() = Foo()
            fun invoke() = given<@Composable () -> Foo>()
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMultipleProvidersInSetWithDependencyDerivedByProviderArgument() = codegen(
        """
            typealias MyGivenScope = GivenScope
            @Given val @Given MyGivenScope.key: String get() = ""
            @Given fun foo(@Given key: String) = Foo()
            @Given fun fooIntoSet(@Given provider: (@Given MyGivenScope) -> Foo): (MyGivenScope) -> Any = provider as (MyGivenScope) -> Any 
            @Given class Dep(@Given key: String)
            @Given fun depIntoSet(@Given provider: (@Given MyGivenScope) -> Dep): (MyGivenScope) -> Any = provider as (MyGivenScope) -> Any
            fun invoke() {
                given<Set<(MyGivenScope) -> Any>>()
            }
        """
    )

    @Test
    fun testProviderWhichReturnsItsParameter() = codegen(
        """
            @Given val foo = Foo()
            fun invoke(): Foo {
                return given<(@Given Foo) -> Foo>()(Foo())
            }
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testProviderWithoutCandidatesError() = codegen(
        """
            fun invoke() {
                given<() -> Foo>()
            }
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type kotlin.Function0<com.ivianuu.injekt.test.Foo> for parameter value of function com.ivianuu.injekt.given")
    }

}