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

class InjectableSetTest {
  @Test fun testSet() = singleAndMultiCodegen(
    """
            @Provide fun commandA() = CommandA()
            @Provide fun commandsB() = setOf(CommandB())
    """,
    """
        fun invoke() = inject<Set<Command>>() 
    """
  ) {
    val set = invokeSingleFile<Set<Command>>().toList()
    set.size shouldBe 2
    set[0].shouldBeTypeOf<CommandA>()
    set[1].shouldBeTypeOf<CommandB>()
  }

  @Test fun testNestedSet() = singleAndMultiCodegen(
    """
            @Provide fun commandA() = CommandA()

            class InnerObject {
                @Provide fun commandsB() = listOf(CommandB())
                val set = inject<Set<Command>>()
            }
    """,
    """
        fun invoke() = inject<Set<Command>>() to InnerObject().set 
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
            @Provide fun commandA() = CommandA()
    """,
    """
         fun invoke() = inject<Set<Command>>() 
    """
  ) {
    irShouldContain(1, "setOf")
  }

  @Test fun testSetWithSingleCollectionElement() = singleAndMultiCodegen(
    """
            @Provide fun commandA() = listOf(CommandA())
    """,
    """
        fun invoke() = inject<Set<Command>>() 
    """
  ) {
    irShouldContain(1, "toSet")
  }

  @Test fun testSetWithSingleSetCollectionElement() = singleAndMultiCodegen(
    """
            @Provide fun commandA() = setOf(CommandA())
    """,
    """
        fun invoke() = inject<Set<Command>>() 
    """
  ) {
    irShouldContain(1, "inject<Set<Command>>(value = commandA())")
  }

  @Test fun testSetWithoutElements() = codegen(
    """
         fun invoke() = inject<Set<Command>>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.collections.Set<com.ivianuu.injekt.test.Command> for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testImplicitProviderSet() = singleAndMultiCodegen(
    """
            @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
        fun invoke() = inject<Set<(@Provide Foo) -> Bar>>() 
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
            @Provide fun bar(foo: Foo): Any = Bar(foo)

            @Provide fun commandA(): Command = CommandA()

            class InnerObject {
                @Provide fun commandB(): Command = CommandB()
                val set = inject<Set<() -> Command>>()
            }
    """,
    """
        fun invoke() = inject<Set<() -> Command>>() to InnerObject().set 
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
                return inject<(@Provide String, @Provide String) -> Set<String>>()("a", "b")
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
            @Provide val a = "a"
            @Provide fun b(foo: Foo) = "b"
    """,
    """
        fun invoke(): @IgnoreElementsWithErrors Set<String> = inject() 
    """
  ) {
    val set = invokeSingleFile<Set<Any>>().toList()
    set.shouldHaveSize(1)
    set[0] shouldBe "a"
  }
}
