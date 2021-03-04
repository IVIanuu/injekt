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

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class ModuleTest {

    @Test
    fun testClassModule() = codegen(
        """
            @Given val foo = Foo()
            @Module class BarModule(@Given private val foo: Foo) {
                @Given val bar get() = Bar(foo)
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testObjectModule() = codegen(
        """
            @Given val foo = Foo()
            @Module object BarModule {
                @Given fun bar(@Given foo: Foo) = Bar(foo)
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testModuleLambdaParameter() = codegen(
        """
            class MyModule {
                @Given val foo = Foo()
            }

            @Given fun foo() = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)

            inline fun <R> withModule(
                block: (@Module MyModule) -> R
            ): R = block(MyModule())

            fun invoke() = withModule { 
                given<Bar>()
            }
        """
    )

    @Test
    fun testGenericModule() = codegen(
        """
            class MyModule<T>(private val instance: T) {
                @Given fun provide() = instance to instance
            }
            @Module val fooModule = MyModule(Foo())
            @Module val stringModule = MyModule("__")
            fun invoke() = given<Pair<Foo, Foo>>()
        """
    )

    @Test
    fun testGenericModuleMulti() = multiCodegen(
        listOf(
            source(
                """
                    class MyModule<T>(private val instance: T) {
                        @Given fun provide() = instance to instance
                    }

                    @Module val fooModule = MyModule(Foo())
                    @Module val stringModule = MyModule("__")
                """
            )
        ),
        listOf(
            source(
                """
                   fun invoke() = given<Pair<Foo, Foo>>() 
                """
            )
        )
    )

    @Test
    fun testGenericModuleQualifiedMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Qualifier annotation class MyQualifier<T>
                    class MyModule<T>(private val instance: T) {
                        @Given fun provide(): @MyQualifier<Int> Pair<T, T> = instance to instance
                    }

                    @Module val fooModule = MyModule(Foo())
                    @Module val stringModule = MyModule("__")
                """
            )
        ),
        listOf(
            source(
                """
                   fun invoke() = given<@MyQualifier<Int> Pair<Foo, Foo>>() 
                """
            )
        )
    )

    @Test
    fun testGenericModuleClass() = codegen(
        """
            @Module
            class MyModule<T> {
                @Given fun provide(@Given instance: T) = instance to instance
            }

            @Given val foo = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke() {
                given<Pair<Foo, Foo>>()
                given<Pair<Bar, Bar>>()
            }
        """
    )

    @Test
    fun testGenericModuleFunction() = codegen(
        """
            class MyModule<T> {
                @Given fun provide(@Given instance: T) = instance to instance
            }

            @Module fun <T> myModule() = MyModule<T>()

            @Given val foo = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke() {
                given<Pair<Foo, Foo>>()
                given<Pair<Bar, Bar>>()
            }
        """
    )

}
