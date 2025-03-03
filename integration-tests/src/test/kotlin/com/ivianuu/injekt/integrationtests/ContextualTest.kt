/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.*
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

class ContextualTest {
  @Test fun testSimple() = codegen(
    """
      @Contextual fun doRequest(): String {
        return anotherOne()
      }

      @Contextual fun anotherOne(): String {
        return run { inject<String>() }
      }
    """,
    """
      @Contextual fun b(): String {
        return doRequest()
      }

      fun invoke(@Provide input: String): String {
        return b()
      }
    """
  ) {
    compilationShouldBeOk()
    invokeSingleFile("hello") shouldBe "hello"
  }

  @Test fun testContextualFunctionRequiresNonContextualRequest() = singleAndMultiCodegen(
    """
      @Provide fun bar(foo: Foo) = Bar(foo)      

      @Contextual fun myFunction() {
        inject<Bar>()
      }
    """,
    """
      fun invoke() {
        @Provide val bar = Bar(Foo())
        myFunction()
      }
    """
  )

  @Test fun testContextualInjectable() = singleAndMultiCodegen(
    """
      @Provide @Contextual fun bar() = Bar(inject())
      @Provide val foo = Foo()
    """,
    """
      fun invoke() = inject<Bar>()
    """
  )

  @Test fun testContextualWithSourceKey() = singleAndMultiCodegen(
    """
      @Contextual fun sourceKeyOfContextual() = sourceKey()
    """,
    """
      fun invoke() = sourceKeyOfContextual()
    """
  ) {
    invokeSingleFile() shouldBe "File0.kt:11:48"
  }

  @Test fun testContextualWithExplicitInjectParameter() = singleAndMultiCodegen(
    """
      @Contextual fun explicitSourceKeyOfContextual(sourceKey: SourceKey = inject) = sourceKey
    """,
    """
      fun invoke() = explicitSourceKeyOfContextual()
    """
  ) {
    invokeSingleFile() shouldBe "File.kt:11:21"
  }
}
