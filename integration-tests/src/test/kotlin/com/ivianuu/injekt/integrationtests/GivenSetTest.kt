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
import com.ivianuu.injekt.test.Command
import com.ivianuu.injekt.test.CommandA
import com.ivianuu.injekt.test.CommandB
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class GivenSetTest {

    @Test
    fun testSimpleSet() = codegen(
        """
            @Given fun commandA(): Command = CommandA()
            @Given fun commandB(): Command = CommandB() 
            fun invoke() = given<Set<Command>>()
        """
    ) {
        val set = invokeSingleFile<Set<Command>>().toList()
        2 shouldBe set.size
        set
            .filterIsInstance<CommandA>()
            .shouldHaveSize(1)
        set
            .filterIsInstance<CommandB>()
            .shouldHaveSize(1)
    }

    @Test
    fun testNestedSet() = codegen(
        """
            @Given fun commandA(): Command = CommandA()

            class InnerObject {
                @Given fun commandB(): Command = CommandB()
                val set = given<Set<Command>>()
            }

            fun invoke() = given<Set<Command>>() to InnerObject().set
        """
    ) {
        val (parentSet, childSet) = invokeSingleFile<Pair<Set<Command>, Set<Command>>>().toList()
        1 shouldBe parentSet.size
        parentSet
            .filterIsInstance<CommandA>()
            .shouldHaveSize(1)
        2 shouldBe childSet.size
        childSet
            .filterIsInstance<CommandA>()
            .shouldHaveSize(1)
        childSet
            .filterIsInstance<CommandB>()
            .shouldHaveSize(1)
    }

    @Test
    fun testEmptyDefault() = codegen(
        """
            fun invoke() = given<Set<Command>>()
        """
    ) {
        emptySet<Command>() shouldBe invokeSingleFile()
    }

    @Test
    fun testImplicitProviderSet() = codegen(
        """
            @Given
            fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke() = given<Set<(@Given Foo) -> Bar>>()
        """
    ) {
        val set = invokeSingleFile<Set<(Foo) -> Bar>>().toList()
        1 shouldBe set.size
        val provider = set.single()
        val foo = Foo()
        val bar = provider(foo)
        foo shouldBeSameInstanceAs bar.foo
    }

    @Test
    fun testNestedImplicitProviderSet() = codegen(
        """
            @Given
            fun bar(@Given foo: Foo): Any = Bar(foo)

            @Given fun commandA(): Command = CommandA()

            class InnerObject {
                @Given fun commandB(): Command = CommandB()
                val set = given<Set<() -> Command>>()
            }

            fun invoke() = given<Set<() -> Command>>() to InnerObject().set
        """
    ) {
        val (parentSet, childSet) = invokeSingleFile<Pair<Set<() -> Command>, Set<() -> Command>>>().toList()
        1 shouldBe parentSet.size
        parentSet
            .map { it() }
            .filterIsInstance<CommandA>()
            .shouldHaveSize(1)
        2 shouldBe childSet.size
        childSet
            .map { it() }
            .filterIsInstance<CommandA>()
            .shouldHaveSize(1)
        childSet
            .map { it() }
            .filterIsInstance<CommandB>()
            .shouldHaveSize(1)
    }

    @Test
    fun testPrefersExplicitProviderSetOverImplicitProviderSet() = codegen(
        """
            @Given
            lateinit var explicitProviderElement: () -> Foo

            @Given
            val nonProviderElement = Foo()
            fun invoke(explicitProvider: () -> Foo): Set<() -> Foo> {
                explicitProviderElement = explicitProvider
                return given<Set<() -> Foo>>()
            }
        """
    ) {
        val explicitProvider: () -> Foo = { Foo() }
        val set = invokeSingleFile<Set<() -> Foo>>(explicitProvider)
        explicitProvider shouldBeSameInstanceAs set.single()
    }

}
