/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.junit.*

class AddOnInjectableTest {
  @Test fun testAddOnInjectableFunction() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class Trigger
      @Provide fun <@AddOn T : @Trigger S, S> triggerImpl(instance: T): S = instance

      @Provide fun foo(): @Trigger Foo = Foo()
    """,
    """
      fun invoke() = inject<Foo>() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testAddOnInjectableClass() = singleAndMultiCodegen(
    """
      @Provide class MyModule<@AddOn T : @Trigger S, S> {
        @Provide fun intoSet(instance: T): @Final S = instance
      }
      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class Trigger

      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class Final

      @Provide fun foo(): @Trigger Foo = Foo()
      @Provide fun string(): @Trigger String = ""
    """,
    """
      fun invoke() = inject<List<@Final Foo>>() 
    """
  ) {
    invokeSingleFile<List<Foo>>().size shouldBe 1
  }

  @Test fun testMultipleAddOnTypeParameters() = codegen(
    """
      @Provide fun <@AddOn T, @AddOn S> triggerImpl() = Unit
    """
  ) {
    compilationShouldHaveFailed("only one @AddOn")
  }

  @Test fun testAddOnInjectableForClass() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class Trigger
      @Provide fun <@AddOn T : @Trigger S, S> triggerImpl(instance: T): S = instance
      
      @Trigger @Provide class NotAny
    """,
    """
      fun invoke() = inject<NotAny>() 
    """
  ) {
    invokeSingleFile()
  }

  @Test fun testAddOnInjectableChain() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE) 
      annotation class A
      
      @Provide fun <@AddOn T : @A S, S> aImpl() = AModule_<S>()
      
      class AModule_<T> {
        @Provide fun my(instance: T): @B T = instance
      }
      
      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class B
      @Provide fun <@AddOn T : @B S, S> bImpl() = BModule_<T>()
      
      class BModule_<T> {
        @Provide fun my(instance: T): @C Any? = instance
      }
      
      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class C
      @Provide fun <@AddOn T : @C Any?> cImpl() = Foo()
      
      @Provide fun dummy(): @A Long = 0L
    """,
    """
      fun invoke() = inject<List<Foo>>().single() 
    """
  ) {
    invokeSingleFile().shouldBeTypeOf<Foo>()
  }

  @Test fun testMultipleAddOnCandidatesWithSameType() = singleAndMultiCodegen(
    """
      @Tag @Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPE)
      annotation class Trigger
      @Provide fun <@AddOn T : @Trigger String> triggerImpl(instance: T): String = instance

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

  @Test fun testRecursiveAddOnInjectables() = singleAndMultiCodegen(
    """
      object Result

      @Provide fun <@AddOn T : Foo> a(): Result = Result
      @Provide fun <@AddOn T : Foo> b(): Result = Result

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
