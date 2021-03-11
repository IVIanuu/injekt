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
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.irShouldNotContain
import org.junit.Test

class ExpressionWrappingTest {

    @Test
    fun testDoesFunctionWrapGivenWithMultipleUsages() = codegen(
        """
            @Given val foo = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            @Given fun <T> pair(@Given a: T, @Given b: T): Pair<T, T> = a to b
            fun invoke() {
                given<Pair<Bar, Bar>>()
            }
        """
    ) {
        irShouldContain(1, "bar(foo = ")
    }

    @Test
    fun testDoesFunctionWrapGivenWithMultipleUsagesInDifferentScopes() = codegen(
        """
            @Given val foo = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            @Given fun <T> pair(@Given a: T, @Given b: () -> T): Pair<T, () -> T> = a to b
            fun invoke() {
                given<Pair<Bar, () -> Bar>>()
            }
        """
    ) {
        irShouldContain(1, "bar(foo = ")
    }

    @Test
    fun testDoesNotFunctionWrapGivenWithSingleUsage() = codegen(
        """
            @Given val foo = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            fun invoke() {
                given<Bar>()
            }
        """
    ) {
        irShouldNotContain("local fun <anonymous>(): Bar {")
    }

    @Test
    fun testDoesNotWrapGivenWithMultipleUsagesButWithoutDependencies() = codegen(
        """
            @Given val foo = Foo()
            @Given fun <T> pair(@Given a: T, @Given b: T): Pair<T, T> = a to b
            fun invoke() {
                given<Pair<Foo, Foo>>()
            }
        """
    ) {
        irShouldNotContain("local fun <anonymous>(): Foo {")
    }

    @Test
    fun testDoesCacheProviderWithMultipleUsages() = codegen(
        """
            @Given val foo = Foo()
            @Given fun <T> pair(@Given a: T, @Given b: T): Pair<T, T> = a to b
            fun invoke() {
                given<Pair<() -> Foo, () -> Foo>>()
            }
        """
    ) {
        irShouldNotContain("local fun <anonymous>(): Function0<Foo> {")
        irShouldContain(1, "val tmp0: Function0<Foo> = local fun <anonymous>(): Foo {")
    }

    @Test
    fun testDoesNotCacheProviderWithSingleUsage() = codegen(
        """
            @Given val foo = Foo()
            fun invoke() {
                given<() -> Foo>()
            }
        """
    ) {
        irShouldNotContain("val tmp0: Function0<Foo> = local fun <anonymous>(): Foo {")
    }

    /*@Test
    fun testDoesNotCacheCircularDependency() = codegen(
        """
            @Given class A(@Given b: B)
            @Given class B(@Given a: () -> A)
            fun invoke() = given<B>()
       """
    ) {

    }

    @Test
    fun testDoesNotFunctionWrapCircularDependency() = codegen(
        """
            @Given class A(@Given b: B)
            @Given class B(@Given a: () -> A)
            fun invoke() = given<B>()
       """
    ) {
        invokeSingleFile()
    }*/

    // todo does not cache circular dependency

}