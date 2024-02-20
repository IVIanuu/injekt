/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests.legacy

import com.ivianuu.injekt.integrationtests.*
import io.kotest.matchers.*
import io.kotest.matchers.types.*
import org.junit.*

class ListTest {
  @Test fun testList() = singleAndMultiCodegen(
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
    compilationShouldHaveFailed("no injectable")
  }
}
