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
import org.junit.Test

class FunctionWrappingTest {

    @Test
    fun testFunctionWrapping() = codegen(
        """
            @Given val foo = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            @Given fun <T> pair(@Given a: T, @Given b: T): Pair<T, T> = a to b
            fun invoke() {
                given<Pair<Bar, Bar>>()
            }
        """
    )

    @Test
    fun testFunctionWrappingInProvider() = codegen(
        """
            @Given val foo = Foo()
            @Given fun bar(@Given foo: Foo) = Bar(foo)
            @Given fun <T> pair(@Given a: T, @Given b: (@Given Foo) -> T): Pair<T, (Foo) -> T> = a to b
            fun invoke() {
                given<(@Given Foo) -> Pair<Bar, (Foo) -> Bar>>()(Foo())
            }
        """
    )

    @Test
    fun testStableDependencyWithDefaultValueAndMixedAvailability() = codegen(
        """
            @Given fun bar(@Given foo: Foo = Foo()) = Bar(foo)
            @Given class Dep1(@Given val bar: Bar)
            @Given class Dep2(@Given val bar: Bar)
            @Given fun pair(@Given a: Dep1, @Given b: (@Given Foo) -> Dep2): Pair<Dep1, (Foo) -> Dep2> = a to b
            fun invoke() {
                given<Pair<Dep1, (Foo) -> Dep2>>()
            }
        """
    )

}
