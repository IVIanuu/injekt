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
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertTrue
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
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testProviderWithGivenArgs() = codegen(
        """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            fun invoke() = given<(@Given Foo) -> Bar>()(Foo())
        """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testProviderWithGenericGivenArgs() = codegen(
        """ 
            typealias ComponentA = Component

            fun createComponentA() = ComponentBuilder<ComponentA>()
                .build()

            typealias ComponentB = Component

            @ComponentElementBinding<ComponentA>
            @Given
            fun componentBFactory(
                @Given parent: ComponentA,
                @Given builderFactory: () -> Component.Builder<ComponentB>
            ): () -> ComponentB = { 
                builderFactory()
                    .dependency(parent)
                    .build()
            }

            typealias ComponentC = Component

            @ComponentElementBinding<ComponentB>
            @Given 
            fun componentCFactory(
                @Given parent: ComponentB,
                @Given builderFactory: () -> Component.Builder<ComponentC>
            ): () -> ComponentC = {
                builderFactory()
                    .dependency(parent)
                    .build()
            }

            @ComponentElementBinding<ComponentC>
            @Given class MyComponent(
                @Given val a: ComponentA,
                @Given val b: ComponentB,
                @Given val c: ComponentC
            )
            """
    )

    @Test fun testProviderWithGenericGivenArgsMulti() = multiCodegen(
        listOf(
            source(
                """
                    typealias ComponentA = Component
        
                    typealias ComponentB = Component
        
                    @ComponentElementBinding<ComponentA>
                    @Given
                    fun componentBFactory(
                        @Given parent: ComponentA,
                        @Given builderFactory: () -> Component.Builder<ComponentB>
                    ): () -> ComponentB = { 
                        builderFactory()
                            .dependency(parent)
                            .build()
                    }
        
                    typealias ComponentC = Component
        
                    @ComponentElementBinding<ComponentB>
                    @Given 
                    fun componentCFactory(
                        @Given parent: ComponentB,
                        @Given builderFactory: () -> Component.Builder<ComponentC>
                    ): () -> ComponentC = {
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
                    fun createComponentA() = ComponentBuilder<ComponentA>().build()

                    @ComponentElementBinding<ComponentC>
                    @Given class MyComponent(
                        @Given val a: ComponentA,
                        @Given val b: ComponentB,
                        @Given val c: ComponentC
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
                return given<(@Module FooModule) -> Bar>()(FooModule(Foo()))
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
        assertTrue(invokeSingleFile() is Foo)
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
    
}