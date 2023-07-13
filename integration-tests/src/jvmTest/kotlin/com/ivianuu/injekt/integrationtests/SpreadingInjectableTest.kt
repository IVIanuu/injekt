/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test

class SpreadingInjectableTest {
  @Test fun testSpreadingInjectableFunction() = singleAndMultiCodegen(
    """
      @Tag annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance

      @Provide fun foo(): @Trigger Foo = Foo()
    """,
    """
      fun invoke() = context<Foo>() 
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
      fun invoke() = context<List<@Final Foo>>() 
    """
  ) {
    invokeSingleFile<List<Foo>>().size shouldBe 1
  }

  @Test fun testSpreadingNonInjectableClass() = codegen(
    """
      class MyModule<@Spread T>
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Provide functions and @Provide classes")
  }

  @Test fun testSpreadingNonInjectableFunction() = codegen(
    """
      fun <@Spread T> triggerImpl() = Unit
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Provide functions and @Provide classes")
  }

  @Test fun testSpreadingProperty() = codegen(
    """
      val <@Spread T> T.prop get() = Unit
    """
  ) {
    compilationShouldHaveFailed("a @Spread type parameter is only supported on @Provide functions and @Provide classes")
  }

  @Test fun testMultipleSpreadTypeParameters() = codegen(
    """
      @Provide fun <@Spread T, @Spread S> triggerImpl() = Unit
    """
  ) {
    compilationShouldHaveFailed("a declaration may have only one @Spread type parameter")
  }

  @Test fun testSpreadingInjectableTriggeredByClass() = singleAndMultiCodegen(
    """
      @Tag annotation class Trigger
      @Provide fun <@Spread T : @Trigger S, S> triggerImpl(instance: T): S = instance
      
      @Trigger @Provide class NotAny
    """,
    """
      fun invoke() = context<NotAny>() 
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
      fun invoke() = context<List<Foo>>().single() 
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
      fun invoke() = context<List<String>>() 
    """
  ) {
    invokeSingleFile<Set<String>>()
      .shouldContainExactly("a", "b")
  }
}
