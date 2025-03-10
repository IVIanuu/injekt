/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package injekt.integrationtests

import io.kotest.matchers.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class ListTest {
  @Test fun testListInjectable() = singleAndMultiCodegen(
    """
      @Provide fun commandA() = CommandA()

      class InnerObject {
        @Provide fun commandsB() = listOf(CommandB())
        val list = create<List<Command>>()
      }
    """,
    """
        fun invoke() = create<List<Command>>() to InnerObject().list 
    """
  ) {
    val (parentList, childList) = invokeSingleFile<Pair<List<Command>, List<Command>>>()
    parentList.size shouldBe 1
    parentList[0].shouldBeTypeOf<CommandA>()
    childList.size shouldBe 2
    childList[0].shouldBeTypeOf<CommandA>()
    childList[1].shouldBeTypeOf<CommandB>()
  }

  @Test fun testLambdaListInjectable() = singleAndMultiCodegen(
    """
      @Provide fun commandA() = CommandA()
      @Provide fun commandB(): List<() -> Command> = listOf({ CommandB() })
    """,
    """
        fun invoke() = create<List<() -> Command>>() 
    """
  ) {
    val list = invokeSingleFile<List<() -> Command>>()
    list.size shouldBe 2
    list[0].invoke().shouldBeTypeOf<CommandA>()
    list[1].invoke().shouldBeTypeOf<CommandB>()
  }

  @Test fun testListInjectableWithoutElements() = codegen(
    """
      fun invoke() = create<List<Command>>()
    """
  ) {
    compilationShouldHaveFailed("no injectable")
  }
}
