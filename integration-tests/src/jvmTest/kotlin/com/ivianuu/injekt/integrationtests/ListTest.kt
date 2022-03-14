/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import androidx.compose.runtime.*
import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import io.kotest.matchers.types.*
import kotlinx.coroutines.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class ListTest {
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

  @Test fun testListWithoutElements() = codegen(
    """
      fun invoke() = inject<List<Command>>()
    """
  ) {
    compilationShouldHaveFailed("no injectable found of type kotlin.collections.List<com.ivianuu.injekt.test.Command> for parameter x of function com.ivianuu.injekt.inject")
  }

  @Test fun testProviderList() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<List<(Foo) -> Bar>>() 
    """
  ) {
    val list = invokeSingleFile<List<(Foo) -> Bar>>().toList()
    list.size shouldBe 1
    val provider = list.single()
    val foo = Foo()
    val bar = provider(foo)
    foo shouldBeSameInstanceAs bar.foo
  }

  @Test fun testSuspendProviderList() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<List<suspend (Foo) -> Bar>>() 
    """
  ) {
    val list = invokeSingleFile<List<suspend (Foo) -> Bar>>().toList()
    list.size shouldBe 1
    val provider = list.single()
    val foo = Foo()
    val bar = runBlocking { provider(foo) }
    foo shouldBeSameInstanceAs bar.foo
  }

  @Test fun testComposableProviderList() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)
    """,
    """
      fun invoke() = inject<List<@Composable (Foo) -> Bar>>() 
    """,
    config = { withCompose() }
  ) {
    val list = invokeSingleFile<List<@Composable (Foo) -> Bar>>().toList()
    list.size shouldBe 1
    val provider = list.single()
    val foo = Foo()
    println()
    val bar = runComposing { provider(foo) }
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
      fun invoke(): List<Any> = inject<(String, String) -> List<String>>()("a", "b")
    """
  ) {
    val list = invokeSingleFile<List<Any>>().toList()
    list.shouldHaveSize(2)
    list[0] shouldBe "a"
    list[1] shouldBe "b"
  }

  @Test fun testIncludesTypeScopeInList() = singleAndMultiCodegen(
    listOf(
      listOf(
        source(
          """
            @Provide fun commandB() = CommandB()
          """,
          packageFqName = FqName("com.ivianuu.injekt.test")
        )
      ),
      listOf(
        source(
          """
            @Provide fun commandsA() = listOf(CommandA())
          """
        ),
        invokableSource(
          """
            fun invoke() = inject<List<Command>>() 
          """
        )
      )
    )
  ) {
    val list = invokeSingleFile<List<Command>>()
    list.size shouldBe 2
    list[0].shouldBeTypeOf<CommandB>()
    list[1].shouldBeTypeOf<CommandA>()
  }
}
