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
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class DivergenceTest {

    @Test
    fun testUnresolvableDivergence() = codegen(
        """
            interface Wrapper<T> {
                val value: T
            }

            @Given fun <T> unwrapped(@Given wrapped: Wrapper<T>): T = wrapped.value

            fun lol() {
                given<Foo>()
            }
        """
    ) {
        compilationShouldHaveFailed("diverging")
    }

    @Test
    fun testUnresolvableDivergenceWithQualifiers() = codegen(
        """
            @Given fun <T> unwrapped(@Given qualified: @Qualifier1 T): T = qualified
            fun lol() {
                given<Foo>()
            }
        """
    ) {
        compilationShouldHaveFailed("diverging")
    }

    // todo @Test
    fun testUnresolvableDivergenceWithProvidersAndQualifiers() = codegen(
        """
            @Given fun <T> any1(@Given t: () -> @Qualifier1 T): T = t()
            @Given fun <T> any2(@Given t: () -> @Qualifier2("a") T): T = t()
            fun invoke() = given<String>()
        """
    ) {
        compilationShouldHaveFailed("diverging")
    }

    // todo @Test
    fun testUnresolvableDivergenceWithProvidersAndQualifiersMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Given fun <T> any1(@Given t: () -> @Qualifier1 T): T = t()
                    @Given fun <T> any2(@Given t: () -> @Qualifier2("a") T): T = t()
                """
            )
        ),
        listOf(
            source(
                """
                   fun invoke() = given<String>() 
                """
            )
        )
    ) {
        it.last().compilationShouldHaveFailed("diverging")
    }

    @Test
    fun testResolvableDivergence() = codegen(
        """
            interface Wrapper<T> {
                val value: T
            }

            @Given fun <T> unwrapped(@Given wrapped: Wrapper<T>): T = wrapped.value

            @Given fun fooWrapper(): Wrapper<Wrapper<Foo>> = error("")

            fun lol() {
                given<Foo>()
            }
        """
    )

    @Test
    fun testResolvableDivergenceWithQualifiers() = codegen(
        """
            @Given fun <T> unwrapped(@Given qualified: @Qualifier1 T): T = qualified

            @Given fun qualifiedFoo(): @Qualifier1 Foo = error("")

            fun lol() {
                given<Foo>()
            }
        """
    )

    @Test
    fun testCircularDependencyFails() = codegen(
        """
            @Given class A(@Given b: B)
            @Given class B(@Given a: A)
            fun invoke() = given<A>()
        """
    ) {
        compilationShouldHaveFailed("diverging")
    }

    @Test
    fun testSelfDependencyFails() = codegen(
        """
            @Given fun <T> anyFromStream(@Given t: T): T = t
            fun invoke() = given<String>()
        """
    ) {
        compilationShouldHaveFailed("diverging")
    }

    @Test
    fun testProviderBreaksCircularDependency() = codegen(
        """
            @Given class A(@Given b: B)
            @Given class B(@Given a: () -> A)
            fun invoke() = given<B>()
       """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testIrrelevantProviderInChainDoesNotBreakCircularDependency() = codegen(
        """
            @Given class A(@Given b: () -> B)
            @Given class B(@Given b: C)
            @Given class C(@Given b: B)
            fun invoke() = given<C>()
       """
    ) {
        compilationShouldHaveFailed("diverging")
    }

    // todo @Test
    fun testLazyRequestInSetBreaksCircularDependency() = codegen(
        """
            typealias A = () -> Unit
            @Given fun a(@Given b: () -> B): A = {}
            @GivenSetElement fun aIntoSet(@Given a: A): () -> Unit = a
            typealias B = () -> Unit
            @Given fun b(@Given a: () -> A): B = {}
            @GivenSetElement fun bIntoSet(@Given b: B): () -> Unit = b
            fun invoke() = given<Set<() -> Unit>>()
       """
    ) {
        invokeSingleFile()
    }

}