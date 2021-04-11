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
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

class GivenSetTest {
    @Test
    fun testSet() = codegen(
        """
            @Given fun commandA() = CommandA()
            @Given fun commandsB() = setOf(CommandB())
            fun invoke() = given<Set<Command>>()
        """
    ) {
        val set = invokeSingleFile<Set<Command>>().toList()
        set.size shouldBe 2
        set[0].shouldBeTypeOf<CommandA>()
        set[1].shouldBeTypeOf<CommandB>()
    }

    @Test
    fun testNestedSet() = codegen(
        """
            @Given fun commandA() = CommandA()

            class InnerObject {
                @Given fun commandsB() = listOf(CommandB())
                val set = given<Set<Command>>()
            }

            fun invoke() = given<Set<Command>>() to InnerObject().set
        """
    ) {
        val (parentSet, childSet) = invokeSingleFile<Pair<Set<Command>, Set<Command>>>()
            .let { it.first.toList() to it.second.toList() }
        parentSet.size shouldBe 1
        parentSet[0].shouldBeTypeOf<CommandA>()
        childSet.size shouldBe 2
        childSet[0].shouldBeTypeOf<CommandA>()
        childSet[1].shouldBeTypeOf<CommandB>()
    }

    @Test
    fun testSetWithSingleElement() = codegen(
        """
            @Given fun commandA() = CommandA()
            fun invoke() = given<Set<Command>>()
        """
    ) {
        irShouldContain(1, "setOf")
    }

    @Test
    fun testSetWithSingleCollectionElement() = codegen(
        """
            @Given fun commandA() = listOf(CommandA())
            fun invoke() = given<Set<Command>>()
        """
    ) {
        irShouldContain(1, "toSet")
    }

    @Test
    fun testSetWithSingleSetCollectionElement() = codegen(
        """
            @Given fun commandA() = setOf(CommandA())
            fun invoke() = given<Set<Command>>()
        """
    ) {
        irShouldContain(1, "given<Set<Command>>(value = commandA())")
    }

    @Test
    fun testSetWithoutElements() = codegen(
        """
            fun invoke() = given<Set<Command>>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type kotlin.collections.Set<com.ivianuu.injekt.test.Command> for parameter value of function com.ivianuu.injekt.given")
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
        set.size shouldBe 1
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
        val (parentSet, childSet) = invokeSingleFile<Pair<Set<() -> Command>, Set<() -> Command>>>()
            .let { it.first.toList() to it.second.toList() }
        parentSet.size shouldBe 1
        parentSet[0]().shouldBeTypeOf<CommandA>()
        childSet.size shouldBe 2
        childSet[0]().shouldBeTypeOf<CommandA>()
        childSet[1]().shouldBeTypeOf<CommandB>()
    }

    @Test
    fun testUsesAllProviderArgumentsForGivenRequest() = codegen(
        """
            fun invoke(): Set<Any> {
                return given<(@Given String, @Given String) -> Set<String>>()("a", "b")
            }
        """
    ) {
        val set = invokeSingleFile<Set<Any>>().toList()
        set.shouldHaveSize(2)
        set[0] shouldBe "a"
        set[1] shouldBe "b"
    }

    @Test
    fun testSetWithAbstractGiven() = codegen(
        """
            @Given interface MyComponent {
                @Given val foo: Foo
            }  
            @Given val foo = Foo()
            fun invoke(): Set<MyComponent> = given()
        """
    )

    @Test
    fun testSetWithIgnoreElementsWithErrors() = codegen(
        """
            @Given val a = "a"
            @Given fun b(@Given foo: Foo) = "b"
            fun invoke(): @IgnoreElementsWithErrors Set<String> = given()
        """
    ) {
        val set = invokeSingleFile<Set<Any>>().toList()
        set.shouldHaveSize(1)
        set[0] shouldBe "a"
    }
}
