/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests.legacy

import com.ivianuu.injekt.integrationtests.*
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import io.kotest.matchers.types.*
import org.junit.*

class SpreadingInjectableTest {
  @Test fun testSpreadingInjectableFunction() = singleAndMultiCodegen(
    """
      @Tag annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance

      @Provide fun foo(): @Trigger Foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testSpreadingInjectableClass() = singleAndMultiCodegen(
    """
      @Provide class MyModule<@Spread T : @Trigger S, S> {
        @Provide fun intoSet(instance: T): @Final S = instance
      }
      @Tag annotation class Trigger

      @Tag annotation class Final

      @Provide fun foo(): @Trigger Foo = Foo()
      @Provide fun string(): @Trigger String = ""
    """,
    """
      fun invoke() = inject<List<@Final Foo>>() 
    """
  ) {
    invokeSingleFile<List<Foo>>().size shouldBe 1
  }

  @Test fun testMultipleSpreadTypeParameters() = codegen(
    """
      @Provide fun <@Spread T, @Spread S> triggerImpl() = Unit
    """
  ) {
    compilationShouldHaveFailed("only one @Spread")
  }

  @Test fun testSpreadingInjectableTriggeredByClass() = singleAndMultiCodegen(
    """
      @Tag annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance
      
      @Trigger @Provide class NotAny
    """,
    """
      fun invoke() = inject<NotAny>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testSpreadingInjectableChain() = singleAndMultiCodegen(
    """
      @Tag annotation class A
      
      @Provide fun <@Spread T : @A S, S> aImpl() = AModule_<S>()
      
      class AModule_<T> {
        @Provide fun my(instance: T): @B T = instance
      }
      
      @Tag annotation class B
      @Provide fun <@Spread T : @B S, S> bImpl() = BModule_<T>()
      
      class BModule_<T> {
        @Provide fun my(instance: T): @C Any? = instance
      }
      
      @Tag annotation class C
      @Provide fun <@Spread T : @C Any?> cImpl() = Foo()
      
      @Provide fun dummy(): @A Long = 0L
    """,
    """
      fun invoke() = inject<List<Foo>>().single() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testMultipleSpreadCandidatesWithSameType() = singleAndMultiCodegen(
    """
      @Tag annotation class Trigger
      @Provide fun <@Spread T : @Trigger String> triggerImpl(instance: T): String = instance

      @Provide fun a(): @Trigger String = "a"
      @Provide fun b(): @Trigger String = "b"
    """,
    """
      fun invoke() = inject<List<String>>() 
    """
  ) {
    invokeSingleFile<Set<String>>()
      .shouldContainExactly("a", "b")
  }

  @Test fun testRecursiveSpreadingInjectables() = singleAndMultiCodegen(
    """
      object Result

      @Provide fun <@Spread T : Foo> a(): Result = Result
      @Provide fun <@Spread T : Foo> b(): Result = Result

      @Provide fun foo() = Foo()
    """,
    """
      fun invoke() = inject<Result>() 
    """
  ) {
    compilationShouldHaveFailed(":\n" +
        "\n" +
        "com.ivianuu.injekt.integrationtests.a\n" +
        "com.ivianuu.injekt.integrationtests.b\n" +
        "\n" +
        "do")
  }
}
