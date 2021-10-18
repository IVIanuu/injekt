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

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Command
import com.ivianuu.injekt.test.CommandA
import com.ivianuu.injekt.test.CommandB
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.irShouldContain
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

class InjectableListTest {
  @Test fun testList() = singleAndMultiCodegen(
    """
      @Provide fun commandA() = CommandA()
      @Provide fun commandsB() = listOf(CommandB())
    """,
    """
      fun invoke() = inject<List<Command>>() 
    """
  ) {
    val list = invokeSingleFile<List<Command>>()
    list.size shouldBe 2
    list[0].shouldBeTypeOf<CommandA>()
    list[1].shouldBeTypeOf<CommandB>()
  }

  @Test fun testNestedList() = singleAndMultiCodegen(
    """
      @Provide fun commandA() = CommandA()

      class InnerObject {
        @Provide fun commandsB() = listOf(CommandB())
        val list = inject<List<Command>>()
      }
    """,
    """
        fun invoke() = inject<List<Command>>() to InnerObject().list 
    """
  ) {
    val (parentList, childList) = invokeSingleFile<Pair<List<Command>, List<Command>>>()
    parentList.size shouldBe 1
    parentList[0].shouldBeTypeOf<CommandA>()
    childList.size shouldBe 2
    childList[0].shouldBeTypeOf<CommandA>()
    childList[1].shouldBeTypeOf<CommandB>()
  }

  @Test fun testListWithSingleElement() = singleAndMultiCodegen(
    """
      @Provide fun commandA() = CommandA()
    """,
    """
      fun invoke() = inject<List<Command>>() 
    """
  ) {
    irShouldContain(1, "listOf")
  }

  @Test fun testListWithSingleCollectionElement() = singleAndMultiCodegen(
    """
      @Provide fun commandA() = setOf(CommandA())
    """,
    """
      fun invoke() = inject<List<Command>>() 
    """
  ) {
    irShouldContain(1, "toList")
  }

  @Test fun testListWithSingleListCollectionElement() = singleAndMultiCodegen(
    """
      @Provide fun commandA() = listOf(CommandA())
    """,
    """
      fun invoke() = inject<List<Command>>() 
    """
  ) {
    irShouldContain(1, "inject<List<Command>>(value = commandA())")
  }

  @Test fun testListWithoutElements() = codegen(
    """
      fun invoke() = inject<List<Command>>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.collections.List<com.ivianuu.injekt.test.Command> for parameter value of function com.ivianuu.injekt.inject")
  }

  @Test fun testProviderList() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<List<(@Provide Foo) -> Bar>>() 
    """
  ) {
    val list = invokeSingleFile<List<(Foo) -> Bar>>().toList()
    list.size shouldBe 1
    val provider = list.single()
    val foo = Foo()
    val bar = provider(foo)
    foo shouldBeSameInstanceAs bar.foo
  }

  @Test fun testNestedProviderList() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo): Any = Bar(foo)

      @Provide fun commandA(): Command = CommandA()

      class InnerObject {
        @Provide fun commandB(): Command = CommandB()
        val list = inject<List<() -> Command>>()
      }
    """,
    """
      fun invoke() = inject<List<() -> Command>>() to InnerObject().list 
    """
  ) {
    val (parentList, childList) = invokeSingleFile<Pair<List<() -> Command>, List<() -> Command>>>()
      .let { it.first.toList() to it.second.toList() }
    parentList.size shouldBe 1
    parentList[0]().shouldBeTypeOf<CommandA>()
    childList.size shouldBe 2
    childList[0]().shouldBeTypeOf<CommandA>()
    childList[1]().shouldBeTypeOf<CommandB>()
  }

  @Test fun testUsesAllProviderArgumentsForInjectableRequest() = codegen(
    """
      fun invoke(): List<Any> = 
          inject<(@Provide String, @Provide String) -> List<String>>()("a", "b")
    """
  ) {
    val list = invokeSingleFile<List<Any>>().toList()
    list.shouldHaveSize(2)
    list[0] shouldBe "a"
    list[1] shouldBe "b"
  }

  @Test fun testListWithComponent() = singleAndMultiCodegen(
    """
      @Component interface MyComponent {
        val foo: Foo
      }

      @Provide val foo = Foo()
    """,
    """
      fun invoke(): List<MyComponent> = inject()
    """
  )

  @Test fun testProviderListWithComponent() = singleAndMultiCodegen(
    """
      @Component interface MyComponent {
        val foo: Foo
      }

      @Provide val foo = Foo()
    """,
    """
      fun invoke(): List<() -> MyComponent> = inject()
    """
  )
}
