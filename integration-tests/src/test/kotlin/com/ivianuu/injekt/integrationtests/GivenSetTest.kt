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
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import io.kotest.matchers.types.*
import org.junit.*

class GivenSetTest {
  @Test fun testSet() = singleAndMultiCodegen(
    """
            @Given fun commandA() = CommandA()
            @Given fun commandsB() = setOf(CommandB())
    """,
    """
        fun invoke() = given<Set<Command>>() 
    """
  ) {
    val set = invokeSingleFile<Set<Command>>().toList()
    set.size shouldBe 2
    set[0].shouldBeTypeOf<CommandA>()
    set[1].shouldBeTypeOf<CommandB>()
  }

  @Test fun testNestedSet() = singleAndMultiCodegen(
    """
            @Given fun commandA() = CommandA()

            class InnerObject {
                @Given fun commandsB() = listOf(CommandB())
                val set = given<Set<Command>>()
            }
    """,
    """
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

  @Test fun testSetWithSingleElement() = singleAndMultiCodegen(
    """
            @Given fun commandA() = CommandA()
    """,
    """
         fun invoke() = given<Set<Command>>() 
    """
  ) {
    irShouldContain(1, "setOf")
  }

  @Test fun testSetWithSingleCollectionElement() = singleAndMultiCodegen(
    """
            @Given fun commandA() = listOf(CommandA())
    """,
    """
        fun invoke() = given<Set<Command>>() 
    """
  ) {
    irShouldContain(1, "toSet")
  }

  @Test fun testSetWithSingleSetCollectionElement() = singleAndMultiCodegen(
    """
            @Given fun commandA() = setOf(CommandA())
    """,
    """
        fun invoke() = given<Set<Command>>() 
    """
  ) {
    irShouldContain(1, "given<Set<Command>>(value = commandA())")
  }

  @Test fun testSetWithoutElements() = codegen(
    """
         fun invoke() = given<Set<Command>>()
    """
  ) {
    compilationShouldHaveFailed("no given argument found of type kotlin.collections.Set<com.ivianuu.injekt.test.Command> for parameter value of function com.ivianuu.injekt.given")
  }

  @Test fun testImplicitProviderSet() = singleAndMultiCodegen(
    """
            @Given fun bar(@Given foo: Foo) = Bar(foo)
    """,
    """
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

  @Test fun testNestedImplicitProviderSet() = singleAndMultiCodegen(
    """
            @Given fun bar(@Given foo: Foo): Any = Bar(foo)

            @Given fun commandA(): Command = CommandA()

            class InnerObject {
                @Given fun commandB(): Command = CommandB()
                val set = given<Set<() -> Command>>()
            }
    """,
    """
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

  @Test fun testUsesAllProviderArgumentsForGivenRequest() = codegen(
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

  @Test fun testSetWithIgnoreElementsWithErrors() = singleAndMultiCodegen(
    """
            @Given val a = "a"
            @Given fun b(@Given foo: Foo) = "b"
    """,
    """
        fun invoke(): @IgnoreElementsWithErrors Set<String> = given() 
    """
  ) {
    val set = invokeSingleFile<Set<Any>>().toList()
    set.shouldHaveSize(1)
    set[0] shouldBe "a"
  }
}
